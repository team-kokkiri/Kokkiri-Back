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
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();
        String avatar = oAuth2User.getAvatar();

        String teamCode = (String) request.getSession().getAttribute("teamCode");
        if (teamCode == null || teamCode.isEmpty()) {
            response.sendRedirect("http://localhost:8080/teamcode-verify");
            return;
        }

        Optional<Team> optionalTeam = teamRepository.findByTeamCode(teamCode);
        if (optionalTeam.isEmpty()) {
            response.sendRedirect("http://localhost:8080/teamcode-verify");
            return;
        }

        Team team = optionalTeam.get();
        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        if (optionalMember.isPresent()) {
            Member member = optionalMember.get();

            if (member.getTeam() == null) {
                member.setTeam(team);
                memberRepository.save(member);
            }

            String role = member.getRole() != null ? member.getRole().name() : "ROLE_USER";
            String accessToken = jwtUtil.generateToken(email, role, true, member.getNickname(),avatar);
            String refreshToken = jwtUtil.generateToken(email, role, false, member.getNickname(), avatar);
            long refreshTokenExpiry = jwtUtil.getExpiration(refreshToken);

            refreshTokenService.saveRefreshToken(email, refreshToken, refreshTokenExpiry);

            // 리프레시 토큰을 HttpOnly 쿠키로 설정
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)  // HTTPS 환경에서는 true로 변경 필요
                    .path("/")
                    .maxAge(refreshTokenExpiry / 1000)
                    .sameSite("Strict")
                    .build();

            response.addHeader("Set-Cookie", cookie.toString());

            // 리프레시 토큰은 URL에서 제거 액세스 토큰과 사용자 정보만 쿼리에 담아서 리다이렉트
            String redirectUrl = "http://localhost:8080/oauth2-redirect"
                    + "?accessToken=" + accessToken
                    + "&email=" + email
                    + "&role=" + role
                    + "&avatar=" + avatar;

            response.sendRedirect(redirectUrl);
            return;
        }

        // 신규 회원일 경우 팀코드 입력 페이지로 리다이렉트
        response.sendRedirect("http://localhost:8080/teamcode-verify");
    }
}
