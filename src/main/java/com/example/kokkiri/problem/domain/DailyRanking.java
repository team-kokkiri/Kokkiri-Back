package com.example.kokkiri.problem.domain;

import com.example.kokkiri.common.domain.BaseTimeEntity;
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
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"daily_problem_id", "member_id"})
})
public class DailyRanking extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_problem_id", nullable = false)
    private DailyProblem dailyProblem;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private ProblemSubmission submission;
    
    private Integer rankPosition;           // 순위
    
    @Column(nullable = false)
    private LocalDateTime solveTime;       // 문제 해결 시간
    
    @Builder.Default
    private Integer submissionCount = 1;   // 제출 횟수 (정답까지)
    
    private Integer executionTime;         // 실행 시간 (ms)
}
