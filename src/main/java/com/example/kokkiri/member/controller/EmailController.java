package com.example.kokkiri.member.controller;

import com.example.kokkiri.member.dto.MemberSignupReqDto;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.member.service.EmailService;
import com.example.kokkiri.member.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final MemberRepository memberRepository;  // 이메일 존재 확인용
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MemberService memberService;

    // 인증 코드 발송 API: 회원가입, 비밀번호 재설정 용도 구분(type)
    @PostMapping("/send")
    public ResponseEntity<String> sendVerificationCode(@RequestParam String email,
                                                       @RequestParam String type) {
        try {
            // type 별 이메일 존재 여부 체크
            if ("signup".equals(type)) {
                // 회원가입이면 이미 가입된 이메일은 보내면 안됨
                if (memberRepository.findByEmail(email).isPresent()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 가입된 이메일입니다.");
                }
            } else if ("reset".equals(type)) {
                // 비밀번호 재설정이면 이메일이 가입된 상태여야 함
                if (memberRepository.findByEmail(email).isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("가입된 이메일이 아닙니다.");
                }
            } else {
                // 다른 타입은 처리 안함
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 인증 타입입니다.");
            }

            emailService.sendVerificationCode(email, type);
            return ResponseEntity.ok("인증 코드가 이메일로 발송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("인증 코드 발송에 실패했습니다. 다시 시도  해주세요.");
        }
    }

    // 인증 코드 검증 API: type별 인증 성공 후 Redis에 verified 표시도 분리 처리 가능
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String email,
                                             @RequestParam String code,
                                             @RequestParam String type) {  // HttpSession 제거

        boolean isValid = emailService.verifyCode(email, code, type);

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        try {
            String verifiedKey = "email:verified:" + type + ":" + email;

            if ("signup".equals(type)) {
                String signupKey = "email:temp:signup:" + email;
                String signupJson = redisTemplate.opsForValue().get(signupKey);

                if (signupJson == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("회원가입 정보가 만료되었거나 존재하지 않습니다.");
                }

                MemberSignupReqDto signupDto = objectMapper.readValue(signupJson, MemberSignupReqDto.class);

                // Redis에서 teamCode 꺼내기
                String redisTeamCode = redisTemplate.opsForValue().get("state:teamCode:" + signupDto.getState());
                if (redisTeamCode == null || redisTeamCode.isBlank()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("팀 코드가 존재하지 않습니다.");
                }

                // signupDto에 teamCode 세팅 (기존에 없으면 setter 또는 생성자 활용)
                signupDto.setTeamCode(redisTeamCode);

                // HttpSession 대신 teamCode가 포함된 DTO로 signup 호출
                memberService.signup(signupDto);

                // Redis 키 정리
                redisTemplate.delete(signupKey);
                redisTemplate.delete(verifiedKey);
                redisTemplate.delete("teamCode:" + email);  // 팀코드도 삭제
            }

            return ResponseEntity.ok("이메일 인증 및 회원가입이 완료되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 처리 중 오류가 발생했습니다.");
        }
    }

}