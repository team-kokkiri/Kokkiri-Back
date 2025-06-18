package com.example.kokkiri.common.oauth;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = (String) oAuth2User.getAttributes().get("email");

        Optional<Member> existingUser = memberRepository.findByEmail(email);
        Team team = teamRepository.findByTeamCode("oauth2")
                .orElseThrow(() -> new RuntimeException("oauth2 팀이 없습니다."));

        if (existingUser.isEmpty()) {
            Member newUser = Member.builder()
                    .email(email)
                    .password("") // OAuth2 사용자라면 비번 없음
                    .nickname("구글사용자_" + UUID.randomUUID().toString().substring(0, 6))
                    .role(com.example.kokkiri.member.domain.Role.USER)
                    .provider("google")
                    .team(team)  // 임시 팀코드
                    .build();

            memberRepository.save(newUser);
        }

        return new CustomOAuth2User(oAuth2User.getAttributes());
    }
}