package com.example.kokkiri.common.service;


import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialDataLoader implements CommandLineRunner {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TeamRepository teamRepository;

    private void createTestUser(String email, String nickname, Role role, Team team) {
        if (memberRepository.findByEmail(email).isEmpty()) {
            memberRepository.save(Member.builder()
                    .email(email)
                    .password(passwordEncoder.encode("1234"))
                    .nickname(nickname)
                    .role(role)
                    .team(team)
                    .build());
        }
    }
    @Override
    public void run(String... args) throws Exception {

        Team testTeam = teamRepository.findByTeamCode("test")
                .orElseGet(() -> teamRepository.save(Team.builder()
                        .teamCode("test")
                        .teamName("testTeam")
                        .build()));

        createTestUser("admin@naver.com", "admin", Role.ADMIN, testTeam);

        for (int i = 1; i <= 30; i++) {
            createTestUser("test" + i + "@naver.com", "test" + i, Role.USER, testTeam);
        }


    }
}

