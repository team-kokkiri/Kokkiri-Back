package com.example.kokkiri.common.service;


import com.example.kokkiri.board.domain.BoardType;
import com.example.kokkiri.board.repository.BoardTypeRepository;
import com.example.kokkiri.calendar.domain.CalendarEntity;
import com.example.kokkiri.calendar.repository.CalendarRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.domain.DailyRanking;
import com.example.kokkiri.problem.domain.ProblemSubmission;
import com.example.kokkiri.problem.domain.SubmissionStatus;
import com.example.kokkiri.problem.repository.DailyProblemRepository;
import com.example.kokkiri.problem.repository.DailyRankingRepository;
import com.example.kokkiri.problem.repository.ProblemSubmissionRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Autowired
    private DailyProblemRepository dailyProblemRepository;
    @Autowired
    private DailyRankingRepository dailyRankingRepository;
    @Autowired
    private ProblemSubmissionRepository problemSubmissionRepository;


    private void createTestUser(String email, String nickname, Role role, Team team) {
        if (memberRepository.findByEmailAndIsDeleted

(email,"N").isEmpty()) {
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

    private void insertDailyProblemData() {
        if (dailyProblemRepository.count() == 0) {
            // 오늘 날짜로 샘플 문제 생성
            DailyProblem todayProblem = DailyProblem.builder()
                    .problemDate(LocalDate.now())
                    .title("두 수의 합")
                    .description("두 정수 A와 B를 입력받은 다음, A+B를 출력하는 프로그램을 작성하시오.")
                    .inputDescription("첫째 줄에 A와 B가 주어진다. (0 < A, B < 10)")
                    .outputDescription("첫째 줄에 A+B를 출력한다.")
                    .sampleInput("1 2")
                    .sampleOutput("3")
                    .timeLimit(1000)
                    .memoryLimit(128)
                    .isActive("Y")
                    .build();
            
            dailyProblemRepository.save(todayProblem);
        }
    }

    private void insertDailyRankingData() {
        if (dailyRankingRepository.count() == 0) {
            // 오늘 문제와 멤버들 조회
            DailyProblem todayProblem = dailyProblemRepository.findTodayProblem().orElse(null);
            if (todayProblem == null) return;
            
            List<Member> members = memberRepository.findAll().stream()
                    .filter(member -> member.getRole() == Role.USER)
                    .limit(4)
                    .toList();
            
            if (members.size() < 4) return;
            
            LocalDateTime baseTime = LocalDateTime.now().minusHours(2);
            
            for (int i = 0; i < 4; i++) {
                Member member = members.get(i);
                
                // 문제 제출 기록 생성
                ProblemSubmission submission = ProblemSubmission.builder()
                        .dailyProblem(todayProblem)
                        .member(member)
                        .sourceCode("import java.util.Scanner;\n\npublic class Solution {\n    public static void main(String[] args) {\n        // 두 수의 합 구하기\n        Scanner sc = new Scanner(System.in);\n        int a = sc.nextInt();\n        int b = sc.nextInt();\n        System.out.println(a + b);\n    }\n}")
                        .language("JAVA")
                        .status(SubmissionStatus.ACCEPTED)
                        .executionTime(100 + (i * 50))
                        .memoryUsage(1024 + (i * 100))
                        .judgeResult("Accepted")
                        .submissionTime(baseTime.plusMinutes(i * 15))
                        .judgeTime(baseTime.plusMinutes(i * 15).plusSeconds(5))
                        .build();
                
                problemSubmissionRepository.save(submission);
                
                // 랭킹 데이터 생성
                DailyRanking ranking = DailyRanking.builder()
                        .dailyProblem(todayProblem)
                        .member(member)
                        .submission(submission)
                        .rankPosition(i + 1)
                        .solveTime(baseTime.plusMinutes(i * 15))
                        .submissionCount(i + 1)
                        .executionTime(100 + (i * 50))
                        .build();
                
                dailyRankingRepository.save(ranking);
            }
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

        Member admin = memberRepository.findByEmailAndIsDeleted

("admin@naver.com","N").orElseThrow();
        insertInitialCalendars(admin, testTeam);

        // Daily Problem 데이터 삽입
        insertDailyProblemData();
        
        // Daily Ranking 데이터 삽입 (문제와 멤버가 생성된 후)
        insertDailyRankingData();

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