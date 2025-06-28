package com.example.kokkiri.common.oauth;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final HttpServletRequest request;

    private static final List<String> DEFAULT_PROFILE_IMAGES = List.of(
            "/images/profiles/default1.png",
            "/images/profiles/default2.png",
            "/images/profiles/default3.png"
    );

    private String generateSimpleNickname() {
        return "고라니" + (new Random().nextInt(20) + 1);
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String email;
        String nameAttributeKey;
        String nickname = generateSimpleNickname();
        String selectedAvatar = DEFAULT_PROFILE_IMAGES.get(new Random().nextInt(DEFAULT_PROFILE_IMAGES.size()));

        // 1. 이메일 추출
        if ("google".equalsIgnoreCase(registrationId)) {
            email = (String) oAuth2User.getAttributes().get("email");
            nameAttributeKey = (String) oAuth2User.getAttributes().get("sub");
        } else if ("kakao".equalsIgnoreCase(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            if (kakaoAccount == null || kakaoAccount.get("email") == null) {
                throw new RuntimeException("OAuth2 공급자에서 이메일을 찾을 수 없습니다.");
            }
            email = (String) kakaoAccount.get("email");
            nameAttributeKey = String.valueOf(oAuth2User.getAttributes().get("id"));
        } else {
            throw new RuntimeException("지원하지 않는 OAuth2 공급자: " + registrationId);
        }

        // 2. 기존 회원이면 바로 리턴
        Optional<Member> existingUser = memberRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            Member member = existingUser.get();
            return new CustomOAuth2User(
                    oAuth2User.getAttributes(),
                    nameAttributeKey,
                    email,
                    registrationId,
                    member.getAvatar(),
                    false
            );
        }

        // 3. 신규 회원 → state 값으로 teamCode 조회 시도
        String state = request.getParameter("state");
        String teamCode = null;
        if (state != null && !state.isBlank()) {
            teamCode = redisTemplate.opsForValue().get("state:teamCode:" + state);
        }

        Member newUser = null;
        if (teamCode != null) {
            // teamCode가 있으면 Team 조회 후 신규 회원 생성
            Team team = teamRepository.findByTeamCode(teamCode)
                    .orElseThrow(() -> new RuntimeException("유효하지 않은 팀 코드입니다."));

            newUser = Member.builder()
                    .email(email)
                    .password("")  // OAuth2 회원은 비밀번호 없음
                    .nickname(nickname)
                    .role(Role.USER)
                    .provider(registrationId)
                    .avatar(selectedAvatar)
                    .team(team)
                    .build();

            memberRepository.save(newUser);
            // Redis에서 사용한 state 삭제
            redisTemplate.delete("state:teamCode:" + state);
        }

        // 신규 회원도 isNewUser=false로 반환하여 바로 로그인 처리되도록 변경
        return new CustomOAuth2User(
                oAuth2User.getAttributes(),
                nameAttributeKey,
                email,
                registrationId,
                newUser != null ? newUser.getAvatar() : selectedAvatar,
                false  // 신규 회원이어도 false 처리
        );
    }
}
