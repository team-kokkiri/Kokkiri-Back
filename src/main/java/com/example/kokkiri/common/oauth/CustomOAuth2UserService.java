package com.example.kokkiri.common.oauth;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final HttpSession httpSession;

    private String generateSimpleNickname() {
        int randomNum = (int) (Math.random() * 20) + 1;
        return "미등록_고라니" + randomNum;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String email;
        String nickname = generateSimpleNickname();
        String nameAttributeKey;

        if ("google".equals(registrationId)) {
            email = (String) oAuth2User.getAttributes().get("email");
            nameAttributeKey = (String) oAuth2User.getAttributes().get("sub"); // 고유 ID
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            if (kakaoAccount == null || kakaoAccount.get("email") == null) {
                throw new RuntimeException("OAuth2 공급자에서 이메일을 찾을 수 없습니다.");
            }
            email = (String) kakaoAccount.get("email");
            nameAttributeKey = String.valueOf(oAuth2User.getAttributes().get("id")); // 카카오 고유 ID
        } else {
            throw new RuntimeException("지원하지 않는 OAuth2 공급자: " + registrationId);
        }

        // 세션에서 teamCode 가져오기 (널 처리)
        String teamCode = (String) httpSession.getAttribute("teamCode");

        // teamCode가 없으면 무조건 예외를 던지지 않고, null 상태로 둠
        Team team = null;
        if (teamCode != null && !teamCode.isBlank()) {
            team = teamRepository.findByTeamCode(teamCode).orElse(null);
        }

        Optional<Member> existingUser = memberRepository.findByEmail(email);

        if (existingUser.isEmpty()) {
            // 신규 회원 생성은 teamCode가 있을 때만 진행
            if (team == null) {
                // teamCode가 없으면 신규회원 생성 안 함. 이후 핸들러에서 처리할 것
                // 여기서는 예외 던지지 않고 그냥 넘어감
            } else {
                Member newUser = Member.builder()
                        .email(email)
                        .password("") // OAuth2 사용자라면 비밀번호 없음
                        .nickname(nickname)
                        .role(com.example.kokkiri.member.domain.Role.USER)
                        .provider(registrationId)
                        .team(team)
                        .build();

                memberRepository.save(newUser);
            }
        }

        System.out.println("OAuth2 registrationId = " + registrationId);
        System.out.println("OAuth2 email = " + email);
        System.out.println("OAuth2 attributes = " + oAuth2User.getAttributes());

        return new CustomOAuth2User(oAuth2User.getAttributes(), nameAttributeKey, email, registrationId);
    }
}
