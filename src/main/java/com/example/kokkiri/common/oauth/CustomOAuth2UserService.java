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
            String avatar = existingUser.get().getAvatar();
            return new CustomOAuth2User(
                    oAuth2User.getAttributes(),
                    nameAttributeKey,
                    email,
                    registrationId,
                    avatar
            );
        }

        // 3. 신규 회원 - Redis에서 state로 팀코드 조회
        String state = request.getParameter("state");
        if (state == null || state.isBlank()) {
            throw new RuntimeException("state 파라미터가 누락되었습니다.");
        }

        String teamCode = redisTemplate.opsForValue().get("state:teamCode:" + state);
        if (teamCode == null) {
            throw new RuntimeException("Redis에서 팀코드를 찾을 수 없습니다. (state=" + state + ")");
        }

        // 4. 팀코드로 Team 조회
        Team team = teamRepository.findByTeamCode(teamCode)
                .orElseThrow(() -> new RuntimeException("팀코드가 유효하지 않습니다."));

        // 5. 신규 회원 생성
        Member newUser = Member.builder()
                .email(email)
                .password("") // OAuth2 회원은 패스워드 없음
                .nickname(nickname)
                .role(Role.USER)
                .provider(registrationId)
                .avatar(selectedAvatar)
                .team(team)
                .build();

        memberRepository.save(newUser);

        return new CustomOAuth2User(
                oAuth2User.getAttributes(),
                nameAttributeKey,
                email,
                registrationId,
                selectedAvatar
        );
    }
}
