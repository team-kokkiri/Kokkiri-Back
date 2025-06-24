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

    // Oauth2 로그인 시 JWT 토큰 생성 및 리다이렉트 처리
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getEmail();
        String avatar = ((CustomOAuth2User) authentication.getPrincipal()).getAvatar();

        String teamCode = (String) request.getSession().getAttribute("teamCode");
        if (teamCode == null || teamCode.isEmpty()) {
            // 팀코드가 없으면 팀코드 입력 페이지로 리다이렉트
            response.sendRedirect("http://localhost:8080/teamcode-verify");
            return;
        }

        Optional<Team> optionalTeam = teamRepository.findByTeamCode(teamCode);
        if (optionalTeam.isEmpty()) {
            // 유효하지 않은 팀코드면 팀코드 입력 페이지로 리다이렉트
            response.sendRedirect("http://localhost:8080/teamcode-verify");
            return;
        }

        Team team = optionalTeam.get();
        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        if (optionalMember.isPresent()) {
            // 기존 회원이면 로그인 처리 및 메인페이지로 리다이렉트
            Member member = optionalMember.get();

            if (member.getTeam() == null) {
                member.setTeam(team);
                memberRepository.save(member);
            }

            String role = "ROLE_USER";
            String accessToken = jwtUtil.generateToken(email, role, true,avatar);
            String refreshToken = jwtUtil.generateToken(email, role, false,avatar);
            long refreshTokenExpiry = jwtUtil.getExpiration(refreshToken);

            refreshTokenService.saveRefreshToken(email, refreshToken, refreshTokenExpiry);

            // 토큰을 쿼리 파라미터로 전달
            String redirectUrl = "http://localhost:8080/oauth2-redirect"
                    + "?accessToken=" + accessToken
                    + "&refreshToken=" + refreshToken
                    + "&email=" + email;

            response.sendRedirect(redirectUrl);
            return;
        }

        // 신규 회원이면 회원가입(팀코드 입력) 페이지로 리다이렉트
        response.sendRedirect("http://localhost:8080/teamcode-verify");
    }
}