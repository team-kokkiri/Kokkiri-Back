package com.example.kokkiri.common.jwt;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    // JWT 토큰 생성 (예: 로그인 성공 시 사용자 이메일 담기)
    // 액세스,리플레시 토큰 발급
    public String generateToken(String email, String role, boolean isAccessToken) {
        long expiration = isAccessToken ? jwtProperties.getAccessExpiration() : jwtProperties.getRefreshExpiration();

        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration * 60 * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecretKey())
                .compact();
    }

    // 토큰에서 이메일 추출
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // 토큰에서 role 추출
    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("Token expired");
        } catch (MalformedJwtException e) {
            System.out.println("Malformed token");
        } catch (Exception e) {
            System.out.println("Invalid token");
        }
        return false;
    }
    
    //리프래시토큰이 언제 만료되는지 알기 위한 시간 저장
    public long getExpiration(String token) {
        return getClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecretKey())
                .parseClaimsJws(token)
                .getBody();
    }
}
