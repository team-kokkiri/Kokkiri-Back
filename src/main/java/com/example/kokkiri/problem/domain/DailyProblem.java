package com.example.kokkiri.problem.domain;

import com.example.kokkiri.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyProblem extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private LocalDate problemDate;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String inputDescription;
    
    @Column(columnDefinition = "TEXT")
    private String outputDescription;
    
    @Column(columnDefinition = "TEXT")
    private String sampleInput;
    
    @Column(columnDefinition = "TEXT")
    private String sampleOutput;
    
    @Builder.Default
    private Integer timeLimit = 1000;
    
    @Builder.Default
    private Integer memoryLimit = 128;
    
    @Builder.Default
    private String isActive = "Y";
    
    @OneToMany(mappedBy = "dailyProblem", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProblemSubmission> submissions = new ArrayList<>();
    
    @OneToMany(mappedBy = "dailyProblem", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DailyRanking> rankings = new ArrayList<>();
    
    @OneToMany(mappedBy = "dailyProblem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNum ASC")
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();
}
