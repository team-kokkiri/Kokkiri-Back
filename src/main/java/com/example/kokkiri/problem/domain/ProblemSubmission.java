package com.example.kokkiri.problem.domain;

import com.example.kokkiri.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemSubmission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_problem_id", nullable = false)
    private DailyProblem dailyProblem;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceCode;
    
    @Builder.Default
    private String language = "JAVA";
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;
    
    private Integer executionTime;          // 실행 시간 (ms)
    private Integer memoryUsage;            // 메모리 사용량 (KB)
    
    @Column(columnDefinition = "TEXT")
    private String judgeResult;             // Judge0 결과 상세
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;            // 에러 메시지
    
    @Builder.Default
    private LocalDateTime submissionTime = LocalDateTime.now();
    
    private LocalDateTime judgeTime;        // 채점 완료 시간
    
    @Column
    private Integer passedTestCaseCount;  // 통과한 테스트케이스 수

    @Column(columnDefinition = "TEXT")
    private String testCaseResults;  // 각 테스트케이스별 결과 JSON
}
