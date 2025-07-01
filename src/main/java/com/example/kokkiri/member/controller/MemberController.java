package com.example.kokkiri.member.controller;

import com.example.kokkiri.common.jwt.JwtResponse;
import com.example.kokkiri.common.jwt.JwtUtil;
import com.example.kokkiri.common.jwt.RefreshTokenService;
import com.example.kokkiri.member.dto.*;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.profile.FileStorageService;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.member.service.EmailService;
import com.example.kokkiri.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Duration;
import java.util.List;
import java.util.Map;


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
    private final FileStorageService fileStorageService;

    //회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody MemberSignupReqDto request, HttpSession session) {
        try {
            String email = request.getEmail();
            String state = request.getState();

            if (state == null || state.isBlank()) {
                return ResponseEntity.badRequest().body("state가 없습니다.");
            }


            String teamCode = redisTemplate.opsForValue().get("state:teamCode:" + state);
            if (teamCode == null || teamCode.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("teamCode가 존재하지 않거나 만료되었습니다.");
            }

            // Redis에 회원 정보 임시 저장
            try {
                String key = "email:temp:signup:" + email;
                ObjectMapper objectMapper = new ObjectMapper();

                MemberSignupReqDto dataToSave = new MemberSignupReqDto();
                dataToSave.setEmail(email);
                dataToSave.setPassword(request.getPassword());
                dataToSave.setNickname(request.getNickname());
                dataToSave.setState(state); // state에서 읽은 팀코드

                String json = objectMapper.writeValueAsString(dataToSave);
                redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(10)); // 10분 저장
            } catch (Exception e) {
                log.error("회원가입 정보 저장 중 오류", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("회원가입 정보 저장 중 오류 발생");
            }

            return ResponseEntity.ok("회원가입 정보가 임시 저장되었습니다. 이메일 인증을 진행해주세요.");
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }


    //로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberLoginReqDto request, HttpServletResponse response) {
        try {
            Member member = memberService.login(request);
            String role = member.getRole().name();
            String avatar = member.getAvatar();
            String accessToken = jwtUtil.generateToken(member.getEmail(), role, true, member.getNickname(), avatar);
            String refreshToken = jwtUtil.generateToken(member.getEmail(), role, false, member.getNickname(), avatar);


            // Redis에 리프레시 토큰 저장
            long refreshTokenExpiry = jwtUtil.getExpiration(refreshToken);
            refreshTokenService.saveRefreshToken(member.getEmail(), refreshToken, refreshTokenExpiry);

            // refreshToken을 HttpOnly, Secure 쿠키로 설정
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true) // HTTPS 환경일 때 true로 설정
                    .path("/")
                    .maxAge(refreshTokenExpiry / 1000) // 밀리초 -> 초 변환
                    .sameSite("Strict") // 필요에 따라 "Lax"로 변경 가능
                    .build();

            response.setHeader("Set-Cookie", cookie.toString());

            log.info(">>> refreshToken: " + refreshToken);
            log.info(">>> 유효성 검사 결과: " + jwtUtil.validateToken(refreshToken));

            // accessToken만 응답 바디에 포함 (refreshToken은 쿠키에 있음)
            return ResponseEntity.ok(new JwtResponse(accessToken, null, member.getEmail(), member.getRole().name(), avatar));
        } catch (IllegalArgumentException e) {
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

        Member member = memberRepository.findByEmailAndIsDeleted
(email, "N")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));

        MemberInfoResDto response = new MemberInfoResDto(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name(),
                member.getAvatar()
        );

        return ResponseEntity.ok(response);
    }

    //마이페이지안에서 (이미지 변경api)
    @PostMapping("/profile-image")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            log.info("프로필 이미지 업로드 시작 - 사용자: {}, 파일명: {}, 파일크기: {} bytes", 
                    principal.getName(), file.getOriginalFilename(), file.getSize());
            
            // 파일 유효성 검사
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
            }
            
            // 파일 크기 체크 (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "파일 크기는 5MB를 초과할 수 없습니다."));
            }
            
            // 파일 타입 체크
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "이미지 파일만 업로드 가능합니다."));
            }

            // 1. 파일 저장
            String imageUrl = fileStorageService.store(file);
            log.info("파일 저장 완료 - URL: {}", imageUrl);

            // 2. 로그인한 멤버 조회
            Member member = memberRepository.findByEmailAndIsDeleted(principal.getName(),"N")
                    .orElseThrow(() -> new UsernameNotFoundException("Member not found"));

            // 3. 프로필 이미지 URL 업데이트
            member.setAvatar(imageUrl);
            memberRepository.save(member);
            
            log.info("프로필 이미지 업데이트 완료 - 사용자: {}, 새 이미지 URL: {}", 
                    principal.getName(), imageUrl);

            // 4. 응답
            return ResponseEntity.ok(Map.of("profileImageUrl", imageUrl));
            
        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패 - 사용자: {}, 오류: {}", principal.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "프로필 이미지 업로드 중 오류가 발생했습니다."));
        }
    }

    // 리프레시 토큰으로 액세스 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        log.info("[TokenRefresh] /refresh 호출, 쿠키 refreshToken: {}", refreshToken);

        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            log.warn("[TokenRefresh] Refresh token이 유효하지 않음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token이 유효하지 않습니다.");
        }

        String email = jwtUtil.getEmailFromToken(refreshToken);
        String storedRefreshToken = refreshTokenService.getRefreshToken(email);

        if (!refreshToken.equals(storedRefreshToken)) {
            log.warn("저장된 Refresh token과 일치하지 않음");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("저장된 Refresh token과 일치하지 않습니다.");
        }

        Member member = memberRepository.findByEmailAndIsDeleted
(email, "N")
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        String newAccessToken = jwtUtil.generateToken(
                email,
                member.getRole().name(),
                true,
                member.getNickname(),
                member.getAvatar()
        );

        log.info("[TokenRefresh] AccessToken 재발급 완료: {} (user: {}, nickname: {})", newAccessToken.substring(0, 30) + "...", email, member.getNickname());

        return ResponseEntity.ok(new JwtResponse(
                newAccessToken,
                null,
                email,
                member.getRole().name(),
                member.getAvatar() // 혹시 response에 avatar도 넣는다면
        ));
    }


    //비밀번호 재설정
    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestBody MemberResetPasswordReqDto request) {
        String email = request.getEmail();

        //이메일인증여부 확인
        if (!emailService.isEmailVerified(email, "reset")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("이메일 인증이 필요합니다.");
        }
        try {
            memberService.resetPassword(email, request.getNewPassword());
            log.info("변경된 비밀번호: " + request.getNewPassword());
            //인증정보삭제
            String VerifiedKey = "email:verified:reset:" + email;
            redisTemplate.delete(VerifiedKey);


            return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다!!");
        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchMember(
            @RequestParam String keyword,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {

        List<MemberSearchResDto> memberSearchResDtos = memberService.searchMember(keyword, lastId, size);
        return new ResponseEntity<>(memberSearchResDtos, HttpStatus.OK);
    }

    @GetMapping("/nickname/check")
    public ResponseEntity<?> checkNickname(@RequestParam String nickname) {
        boolean exists = memberRepository.findByNickname(nickname).isPresent();
        return ResponseEntity.ok(Map.of("available", !exists));
    }

    @PostMapping("/nickname")
    public ResponseEntity<String> updateNickname(@RequestBody MemberNicknameUpdateReqDto request, Authentication authentication){
        String email = authentication.getName(); // 현재 로그인한 사용자 이메일 조회

        try {
            memberService.updateNickname(email, request.getNickname());
            return ResponseEntity.ok("닉네임이 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @RequestBody MemberPasswordChangeReqDto request,
            Authentication authentication) {

        String email = authentication.getName(); // 로그인한 사용자 이메일

        try {
            memberService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok("비밀번호가 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //회원탈퇴(소프트삭제)
    @DeleteMapping
    public ResponseEntity<String> deleteMember(Authentication authentication) {
        String email = authentication.getName();
        Member member = memberRepository.findByEmailAndIsDeleted
(email, "N")
                .orElseThrow(() -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다."));

        memberService.withdraw(member.getId());
        return ResponseEntity.ok("회원 탈퇴(소프트 삭제) 및 개인정보가 익명화되었습니다.");
    }

}

