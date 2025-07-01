package com.example.kokkiri.common.oauth;

import com.example.kokkiri.common.jwt.JwtResponse;
import com.example.kokkiri.common.jwt.JwtUtil;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/oauth2")
public class OAuth2LoginController {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/success")
    public ResponseEntity<?> oauth2Success(
            Authentication authentication,
            HttpServletResponse response
    ) {
        String email = authentication.getName();
        Member member = memberRepository.findByEmailAndIsDeleted

(email,"N")
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        String role = member.getRole().name();
        String accessToken = jwtUtil.generateToken(email, role, true,member.getNickname(),member.getAvatar());
        String refreshToken = jwtUtil.generateToken(email, role, false, member.getNickname(), member.getAvatar());

        // 리프레시 토큰을 쿠키로 내려주기
        long refreshTokenExpiry = jwtUtil.getExpiration(refreshToken);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiry / 1000)
                .sameSite("Strict")
                .build();

        response.setHeader("Set-Cookie", cookie.toString());

        // accessToken만 바디에 전달
        return ResponseEntity.ok(new JwtResponse(accessToken, null, email, role, member.getAvatar()));
    }

}
