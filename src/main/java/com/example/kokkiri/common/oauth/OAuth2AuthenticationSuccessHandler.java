package com.example.kokkiri.common.oauth;

import com.example.kokkiri.common.jwt.JwtUtil;
import com.example.kokkiri.common.jwt.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService  refreshTokenService;


    //Oauth2로그인시 jwt토큰 생성
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // JWT 토큰 생성
        String role = "ROLE_USER";
        String accessToken = jwtUtil.generateToken(email, role, true);
        String refreshToken = jwtUtil.generateToken(email, role, false);

        // 리프래시토큰 만료시간
        long refreshTokenExpiry = jwtUtil.getExpiration(refreshToken);

        // Redis 저장
        refreshTokenService.saveRefreshToken(email, refreshToken, refreshTokenExpiry);

        // 로그
        System.out.println("[OAuth2 로그인 성공] email = " + email);
        System.out.println("AccessToken = " + accessToken);
        System.out.println("RefreshToken = " + refreshToken);

        // JWT 토큰을 응답 헤더에 추가
        response.addHeader("Authorization", "Bearer " + accessToken);

        // 필요 시, 프론트로 리다이렉트 또는 응답 처리 (아래는 예시로 루트로 리다이렉트)
        response.sendRedirect("http://localhost:3000/oauth2/success?token=" + accessToken);
    }
}
