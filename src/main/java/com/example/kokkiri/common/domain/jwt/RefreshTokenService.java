package com.example.kokkiri.common.domain.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    // 리프레시 토큰 저장
    public void saveRefreshToken(String email, String refreshToken, long expirationMillis) {
        redisTemplate.opsForValue().set(
                email,
                refreshToken,
                Duration.ofMillis(expirationMillis)
        );
    }

    // 리프레시 토큰 조회
    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(email);
    }

    // 리프레시 토큰 삭제 (로그아웃 등)
    public void deleteRefreshToken(String email) {
        redisTemplate.delete(email);
    }
}