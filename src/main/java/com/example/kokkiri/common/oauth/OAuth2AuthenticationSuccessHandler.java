package com.example.kokkiri.common.oauth;

import com.example.kokkiri.common.jwt.JwtUtil;
import com.example.kokkiri.common.jwt.RefreshTokenService;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.repository.TeamRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();

        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        // 신규 회원 여부는 DB 기준으로만 판단 (CustomOAuth2User의 isNewUser 체크 제거)
        if (optionalMember.isEmpty()) {
            // 신규 회원이면 팀코드 검증 페이지로 리다이렉트 (회원가입 유도)
            response.sendRedirect(frontendBaseUrl + "/teamcode-verify?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
            return;
        }

        Member member = optionalMember.get();

        String accessToken = jwtUtil.generateToken(email, member.getRole().name(), true, member.getNickname(), member.getAvatar());
        String refreshToken = jwtUtil.generateToken(email, member.getRole().name(), false, member.getNickname(), member.getAvatar());

        refreshTokenService.saveRefreshToken(email, refreshToken, jwtUtil.getExpiration(refreshToken));

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true).secure(true).path("/")
                .maxAge(jwtUtil.getExpiration(refreshToken) / 1000)
                .sameSite("Strict").build();

        response.setHeader("Set-Cookie", refreshCookie.toString());

        String redirectUrl = frontendBaseUrl + "/main-page?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }
}
