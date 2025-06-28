package com.example.kokkiri.common.oauth;

import com.example.kokkiri.common.jwt.JwtUtil;
import com.example.kokkiri.common.jwt.RefreshTokenService;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
import jakarta.servlet.ServletException;
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
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();

        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        if (memberOpt.isPresent()) {
            // 기존 회원이면 JWT 발급 → 메인 페이지로 리디렉트
            Member member = memberOpt.get();

            String accessToken = jwtUtil.generateToken(email, member.getRole().name(), true, member.getNickname(), member.getAvatar());
            String refreshToken = jwtUtil.generateToken(email, member.getRole().name(), false, member.getNickname(), member.getAvatar());

            long refreshTokenExpiry = jwtUtil.getExpiration(refreshToken);
            refreshTokenService.saveRefreshToken(email, refreshToken, refreshTokenExpiry);

            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(refreshTokenExpiry / 1000)
                    .sameSite("Strict")
                    .build();

            response.setHeader("Set-Cookie", refreshCookie.toString());

            String redirectUrl = frontendBaseUrl + "/main-page?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        } else {
            // ✅ 신규 회원이면 state 파라미터로 Redis에서 teamCode 확인 후 리다이렉트
            String state = request.getParameter("state");
            System.out.println("OAuth2 콜백에서 받은 state: " + state);

            if (state == null || state.isBlank()) {
                throw new IllegalArgumentException("state 파라미터가 누락되었습니다.");
            }

            String teamCode = redisTemplate.opsForValue().get("state:teamCode:" + state);
            System.out.println("Redis에서 조회한 팀코드: " + teamCode);
            if (teamCode == null) {
                throw new IllegalArgumentException("Redis에서 팀코드를 찾을 수 없습니다. (state=" + state + ")");
            }

            // ✅ 해당 teamCode가 실제 존재하는지 PostgreSQL에서 체크
            Optional<Team> teamOpt = teamRepository.findByTeamCode(teamCode);
            if (teamOpt.isEmpty()) {
                throw new IllegalArgumentException("유효하지 않은 팀코드입니다.");
            }

            // ✅ 정상 처리: 프론트엔드로 리디렉트 (state만 포함 → 프론트가 다시 Redis에서 teamCode 안심하고 사용 가능)
            String redirectUrl = frontendBaseUrl + "/signup?state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        }
    }
}