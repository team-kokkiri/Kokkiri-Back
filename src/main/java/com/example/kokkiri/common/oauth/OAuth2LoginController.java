package com.example.kokkiri.common.oauth;

import com.example.kokkiri.common.jwt.JwtResponse;
import com.example.kokkiri.common.jwt.JwtUtil;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<?> oauth2Success(Authentication authentication) {
        String email = authentication.getName(); // 구글에서 가져온 이메일
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        String role = member.getRole().name(); // 또는 기본 역할 "ROLE_USER" 등 적절히 세팅

        String accessToken = jwtUtil.generateToken(email, role, true);
        String refreshToken = jwtUtil.generateToken(email, role, false);

        return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken, email));
    }
}
