package com.example.kokkiri.member.controller;

import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.member.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final MemberRepository memberRepository;  // 이메일 존재 확인용

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
                    .body("인증 코드 발송에 실패했습니다. 다시 시도해주세요.");
        }
    }

    // 인증 코드 검증 API: type별 인증 성공 후 Redis에 verified 표시도 분리 처리 가능
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String email,
                                             @RequestParam String code,
                                             @RequestParam String type) {
        boolean isValid = emailService.verifyCode(email, code, type);

        if (isValid) {
            // 인증 성공 시 Redis에 검증 완료 플래그 저장 (type 별로 다르게)
            String verifiedKey = "email:verified:" + type + ":" + email;
            emailService.isEmailVerified(verifiedKey,type);

            return ResponseEntity.ok("이메일 인증 성공!!!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 코드가 올바르지 않거나 만료되었습니다.");
        }
    }
}
