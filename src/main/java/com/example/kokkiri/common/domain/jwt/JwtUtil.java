package com.example.kokkiri.common.domain.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secretKey}") String secretKey,
                   @Value("${jwt.expiration}") long expirationMinutes) {
        // base64로 인코딩된 secretKey를 디코딩해서 Key 객체 생성
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMinutes * 60 * 1000; // 분 -> 밀리초 변환
    }

    // JWT 토큰 생성 (예: 로그인 성공 시 사용자 이메일 담기)
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email) // 토큰의 주인 (여기서는 email)
                .setIssuedAt(now) // 발급 시간
                .setExpiration(expiryDate) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 서명 알고리즘과 키 설정
                .compact();
    }

    // JWT 토큰에서 이메일 추출
    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // JWT 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true; // 예외 없으면 유효
        } catch (JwtException | IllegalArgumentException e) {
            // 유효하지 않음 (만료, 변조 등)
            return false;
        }
    }
}
