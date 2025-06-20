package com.example.kokkiri.member.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    /**
     * 인증 코드 생성 후 이메일 전송 및 Redis 저장
     * @param toEmail 수신자 이메일
     * @param type 인증 타입 (예: signup, resetPassword 등)
     */
    public void sendVerificationCode(String toEmail, String type) {
        String code = createRandomCode();
        saveCodeToRedis(toEmail, code, type);

        log.info("[인증 코드 전송] 이메일: {}, 코드: {}", toEmail, code);
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("[Kokkiri] 이메일 인증 코드입니다.");

            String htmlContent = "<div style='font-family: Arial, sans-serif; font-size: 16px;'>"
                    + "<h2 style='color: #4CAF50;'>Kokkiri 인증 코드</h2>"
                    + "<p>아래 인증 코드를 3분 이내에 입력해주세요.</p>"
                    + "<div style='font-size: 24px; font-weight: bold; margin-top: 10px;'>"
                    + code
                    + "</div>"
                    + "<p style='color: gray; font-size: 12px;'>이 메일은 자동 발송된 메일입니다.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new IllegalStateException("이메일 전송 실패", e);
        }
    }

    /**
     * Redis에서 인증 코드와 비교하여 인증 여부 확인
     * 인증 성공 시 인증 완료 플래그 저장
     * @param email 이메일
     * @param inputCode 사용자가 입력한 인증 코드
     * @param type 인증 타입
     * @return 인증 성공 여부
     */
    public boolean verifyCode(String email, String inputCode, String type) {
        String codeKey = getCodeKey(email, type);
        String savedCode = redisTemplate.opsForValue().get(codeKey);

        log.info("[이메일 인증 검증] email: {}, type: {}, 입력코드: {}, 저장코드: {}", email, type, inputCode, savedCode);

        if (savedCode == null || !savedCode.equals(inputCode)) {
            return false;
        }

        // 인증 성공 → 인증 완료 플래그 저장 (10분 유지)
        String verifiedKey = getVerifiedKey(email, type);
        redisTemplate.opsForValue().set(verifiedKey, "true", Duration.ofMinutes(10));
        redisTemplate.delete(codeKey); // 인증 코드 삭제

        return true;
    }

    /**
     * 인증 완료 여부 조회
     * @param email 이메일
     * @param type 인증 타입
     * @return 인증 완료 여부 (true / false)
     */
    public boolean isEmailVerified(String email, String type) {
        String verifiedKey = getVerifiedKey(email, type);
        String verified = redisTemplate.opsForValue().get(verifiedKey);
        return "true".equals(verified);
    }

    //Redis에 인증 코드 저장 (3분 유지)
    private void saveCodeToRedis(String email, String code, String type) {
        String key = getCodeKey(email, type);
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(1));
    }

    //인증 코드 Redis 키 생성 (일관성 유지)
    private String getCodeKey(String email, String type) {
        return "email:verify:" + type + ":" + email;
    }

    //인증 완료 플래그 Redis 키 생성 (일관성 유지)
    private String getVerifiedKey(String email, String type) {
        return "email:verified:" + type + ":" + email;
    }

    //인증 완료 redis 키 삭제
    public void deleteVerifiedKey(String key) {
        redisTemplate.delete(key);
    }

    //6자리 랜덤 인증 코드 생성
    private String createRandomCode() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }
}
