package com.example.kokkiri.common.service;


import com.example.kokkiri.board.domain.BoardType;
import com.example.kokkiri.board.repository.BoardTypeRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InitialDataLoader implements CommandLineRunner {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private BoardTypeRepository boardTypeRepository;

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

    private void insertBoardTypes() {
        if (boardTypeRepository.count() == 0) {  // 중복 방지
            List<BoardType> boardTypes = List.of(
                    new BoardType(1L, "자유게시판", "N"),
                    new BoardType(2L, "자료공유 게시판", "N"),
                    new BoardType(3L, "HOT 게시판", "N"),
                    new BoardType(4L, "공지사항", "N")
            );
            boardTypeRepository.saveAll(boardTypes);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        insertBoardTypes();

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

