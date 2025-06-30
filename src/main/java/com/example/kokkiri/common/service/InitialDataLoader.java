package com.example.kokkiri.common.service;


import com.example.kokkiri.board.domain.BoardType;
import com.example.kokkiri.board.repository.BoardTypeRepository;
import com.example.kokkiri.calendar.domain.CalendarEntity;
import com.example.kokkiri.calendar.repository.CalendarRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    @Autowired
    private CalendarRepository calendarRepository;

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
                    BoardType.builder().typeName("자유게시판").delYn("N").build(),
                    BoardType.builder().typeName("자료공유 게시판").delYn("N").build(),
                    BoardType.builder().typeName("HOT 게시판").delYn("N").build(),
                    BoardType.builder().typeName("공지사항").delYn("N").build(),
                    BoardType.builder().typeName("프로젝트 소개").delYn("N").build()
            );
            boardTypeRepository.saveAll(boardTypes);
        }
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        insertBoardTypes();

        Team testTeam = teamRepository.findByTeamCode("test")
                .orElseGet(() -> teamRepository.save(Team.builder()
                        .teamCode("test")
                        .teamName("testTeam")
                        .build()));

        createTestUser("admin@naver.com", "admin", Role.ADMIN, testTeam);

        for (int i = 1; i <= 10; i++) {
            createTestUser("test" + i + "@naver.com", "test" + i, Role.USER, testTeam);
        }

        Member admin = memberRepository.findByEmail("admin@naver.com").orElseThrow();
        insertInitialCalendars(admin, testTeam);
    }

    private void insertInitialCalendars(Member admin, Team team) {
        List<CalendarEntity> calendars = List.of(
                CalendarEntity.builder()
                        .title("정전")
                        .description("7/2 정전 예정")
                        .date(LocalDate.of(2025, 7, 2))
                        .isPublic(true)
                        .member(admin)
                        .build(),
                CalendarEntity.builder()
                        .title("정보처리기사자격증")
                        .description("7/19 정보처리기사 자격증 시험")
                        .date(LocalDate.of(2025, 7, 19))
                        .isPublic(true)
                        .member(admin)
                        .build(),
                CalendarEntity.builder()
                        .title("[SW산업협회] 오티아이&엑사아이엔티 SW개발자 과정")
                        .description("입관: 2025-06-04, 수료: 2025-11-27")
                        .date(LocalDate.of(2025, 6, 4))
                        .isPublic(true)
                        .member(admin)
                        .build(),
                CalendarEntity.builder()
                        .title("[SW산업협회] 오티아이&엑사아이엔티 SW개발자 과정 수료")
                        .description("수료일")
                        .date(LocalDate.of(2025, 11, 27))
                        .isPublic(true)
                        .member(admin)
                        .build(),
                CalendarEntity.builder()
                        .title("[AI소프트협회] 오라클 협력사 개발자 과정")
                        .description("입관: 2025-06-25, 수료: 2025-11-28")
                        .date(LocalDate.of(2025, 6, 25))
                        .isPublic(true)
                        .member(admin)
                        .build(),
                CalendarEntity.builder()
                        .title("[AI소프트협회] 오라클 협력사 개발자 과정 수료")
                        .description("수료일")
                        .date(LocalDate.of(2025, 11, 28))
                        .isPublic(true)
                        .member(admin)
                        .build(),
                CalendarEntity.builder()
                        .title("[이것이자바다] MSA 풀스택 3차 입관")
                        .description("입관: 2025-08-04, 수료: 2025-12-23")
                        .date(LocalDate.of(2025, 8, 4))
                        .isPublic(true)
                        .member(admin)
                        .build(),
                CalendarEntity.builder()
                        .title("[이것이자바다] MSA 풀스택 3차 수료")
                        .description("수료일")
                        .date(LocalDate.of(2025, 12, 23))
                        .isPublic(true)
                        .member(admin)
                        .build(),
                CalendarEntity.builder()
                        .title("MSA 풀스택 2차 입관")
                        .description("입관: 2025-03-19, 수료: 2025-09-16")
                        .date(LocalDate.of(2025, 3, 19))
                        .isPublic(true)
                        .member(admin)
                        .build(),
                CalendarEntity.builder()
                        .title("MSA 풀스택 2차 수료")
                        .description("수료일")
                        .date(LocalDate.of(2025, 9, 16))
                        .isPublic(true)
                        .member(admin)
                        .build()
        );
        for (CalendarEntity cal : calendars) {
            boolean exists = calendarRepository.existsByTitleAndDateAndIsPublicTrue(cal.getTitle(), cal.getDate());
            if (!exists) {
                calendarRepository.save(cal);
            }
        }
    }

}