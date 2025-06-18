package com.example.kokkiri.member.controller;

import com.example.kokkiri.common.jwt.JwtResponse;
import com.example.kokkiri.common.jwt.JwtUtil;
import com.example.kokkiri.common.jwt.RefreshTokenService;
import com.example.kokkiri.member.dto.MemberInfoResDto;
import com.example.kokkiri.member.dto.MemberLoginReqDto;
import com.example.kokkiri.member.dto.MemberResetPasswordReqDto;
import com.example.kokkiri.member.dto.MemberSignupReqDto;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.member.service.EmailService;
import com.example.kokkiri.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate redisTemplate; //redis인증여부 확인용
    private final EmailService emailService;

    //회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody MemberSignupReqDto request){
        String email = request.getEmail();

        // EmailService를 통해 인증 완료 여부 체크
        if (!emailService.isEmailVerified(email, "signup")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("이메일 인증이 필요합니다.");
        }

        if (memberRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 가입된 이메일입니다.");
        }

        memberService.signup(request);

        // 인증 정보 Redis에서 삭제 (선택)
        String verifiedKey = "email:verified:signup:" + email;
        redisTemplate.delete(verifiedKey);

        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    //로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberLoginReqDto request){
        try{
            Member member = memberService.login(request);
            String accessToken = jwtUtil.generateToken(member.getEmail());
            String refreshToken = jwtUtil.generateRefreshToken(member.getEmail());

            // 리프레시 토큰 만료 시간 계산 후 Redis에 저장
            long refreshTokenExpiry = jwtUtil.getExpiration(refreshToken);
            refreshTokenService.saveRefreshToken(member.getEmail(), refreshToken, refreshTokenExpiry);

            JwtResponse jwtResponse = new JwtResponse(accessToken, refreshToken, member.getEmail());

            log.info("로그인 성공: {}", member.getEmail());
            log.info("액세스 토큰 발급: {}", accessToken);
            log.info("리프레시 토큰 발급: {}", refreshToken);

            // JwtResponse DTO를 JSON 형태로 응답
            return ResponseEntity.ok(jwtResponse);
        }catch(IllegalArgumentException e){
            System.out.println("로그인 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    //로그아웃 토큰까지삭제
    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        String email = authentication.getName();
        System.out.println("로그아웃 시도: " + email);

        String beforeDelete = refreshTokenService.getRefreshToken(email);
        System.out.println("삭제 전 토큰: " + beforeDelete);
        refreshTokenService.deleteRefreshToken(email); // Redis에서 삭제

        String afterDelete = refreshTokenService.getRefreshToken(email);
        System.out.println("삭제 후 토큰: " + afterDelete);

        return ResponseEntity.ok("로그아웃 성공");
    }
    
    
    //토큰확인
    @GetMapping("/me")
    public ResponseEntity<MemberInfoResDto> getMyInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }

        String email = authentication.getName(); // 현재 설정에서 username이 email이라고 가정

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));

        MemberInfoResDto response = new MemberInfoResDto(
                member.getEmail(),
                member.getNickname(),
                member.getRole().name()
        );

        return ResponseEntity.ok(response);
    }

    // 리프레시 토큰으로 액세스 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Refresh-Token") String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token이 유효하지 않습니다.");
        }

        String email = jwtUtil.getEmailFromToken(refreshToken);
        String storedRefreshToken = refreshTokenService.getRefreshToken(email);

        if (!refreshToken.equals(storedRefreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("저장된 Refresh token과 일치하지 않습니다.");
        }

        // 새로운 access 토큰 발급
        String newAccessToken = jwtUtil.generateToken(email);

        System.out.println("리프레시 성공! 새 AccessToken 발급: " + newAccessToken);

        return ResponseEntity.ok(new JwtResponse(newAccessToken, refreshToken ,email));
    }

    @PostMapping("/reset_Password")
    public ResponseEntity<String> resetPassword(@RequestBody MemberResetPasswordReqDto request) {
        String email = request.getEmail();

        //이메일인증여부 확인
        if (!emailService.isEmailVerified(email, "reset")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("이메일 인증이 필요합니다.");
        }
        try{
            memberService.resetPassword(email, request.getNewPassword());
            log.info("변경된 비밀번호: "+ request.getNewPassword());
            //인증정보삭제
            String VerifiedKey = "email:verified:reset:" + email;
            redisTemplate.delete(VerifiedKey);


            return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다!!");
            }catch (IllegalArgumentException e) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}












