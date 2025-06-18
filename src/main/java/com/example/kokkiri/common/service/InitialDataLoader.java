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

    @Override
    public void run(String... args) throws Exception {
        Team testTeam = null;
        if (teamRepository.findByTeamCode("test").isEmpty()){
            testTeam = Team.builder()
                    .teamCode("test")
                    .teamName("testTeam")
                    .build();
            teamRepository.save(testTeam);
        }


        // ADMIN 계정
        if (memberRepository.findByEmail("admin@naver.com").isEmpty()) {
            Member admin = Member.builder()
                    .email("admin@naver.com")
                    .password(passwordEncoder.encode("1234"))
                    .nickname("admin")
                    .role(Role.ADMIN)
                    .team(testTeam)
                    .build();
            memberRepository.save(admin);
        }

        if (memberRepository.findByEmail("test1@naver.com").isEmpty()) {
            Member test = Member.builder()
                    .email("test1@naver.com")
                    .password(passwordEncoder.encode("1234"))
                    .nickname("test1")
                    .role(Role.USER)
                    .team(testTeam)
                    .build();
            memberRepository.save(test);
        }


        if (memberRepository.findByEmail("test2@naver.com").isEmpty()) {
            Member test = Member.builder()
                    .email("test2@naver.com")
                    .password(passwordEncoder.encode("1234"))
                    .nickname("test2")
                    .role(Role.USER)
                    .team(testTeam)
                    .build();
            memberRepository.save(test);
        }

        if (memberRepository.findByEmail("test3@naver.com").isEmpty()) {
            Member test = Member.builder()
                    .email("test3@naver.com")
                    .password(passwordEncoder.encode("1234"))
                    .nickname("test3")
                    .role(Role.USER)
                    .team(testTeam)
                    .build();
            memberRepository.save(test);
        }
    }
}

