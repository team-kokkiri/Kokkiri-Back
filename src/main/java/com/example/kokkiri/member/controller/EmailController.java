package com.example.kokkiri.member.controller;

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

    // 인증 코드 발송 API: type 파라미터 필수 (signup, reset 등)
    @PostMapping("/send")
    public ResponseEntity<String> sendVerificationCode(@RequestParam String email,
                                                       @RequestParam String type) {
        try {
            emailService.sendVerificationCode(email, type);
            return ResponseEntity.ok("인증 코드가 이메일로 발송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("인증 코드 발송에 실패했습니다. 다시 시도해주세요.");
        }
    }

    // 인증 코드 검증 API: type(인증처리가 분류되어있을경우)
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String email,
                                             @RequestParam String code,
                                             @RequestParam String type) {
        boolean isValid = emailService.verifyCode(email, code, type);

        if (isValid) {
            return ResponseEntity.ok("이메일 인증 성공!!!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 코드가 올바르지 않거나 만료되었습니다.");
        }
    }
}
