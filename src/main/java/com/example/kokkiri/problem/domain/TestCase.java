package com.example.kokkiri.problem.domain;

import com.example.kokkiri.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private DailyProblem dailyProblem;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String input;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;
    
    @Column(nullable = false)
    private Integer orderNum;  // 테스트케이스 순서 (1, 2, 3)
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isHidden = false;  // 히든 테스트케이스 여부
}
