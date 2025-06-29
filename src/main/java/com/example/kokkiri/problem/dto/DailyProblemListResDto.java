package com.example.kokkiri.problem.dto;

import com.example.kokkiri.problem.domain.DailyProblem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class DailyProblemListResDto {
    
    private Long id;
    private LocalDate problemDate;
    private String title;
    private Integer timeLimit;
    private Integer memoryLimit;
    private LocalDateTime createdTime;
    
    // 통계 정보
    private Long solverCount;
    private Boolean hasSolved;
    private Integer mySubmissionCount;
    
    public static DailyProblemListResDto from(DailyProblem dailyProblem) {
        return DailyProblemListResDto.builder()
                .id(dailyProblem.getId())
                .problemDate(dailyProblem.getProblemDate())
                .title(dailyProblem.getTitle())
                .timeLimit(dailyProblem.getTimeLimit())
                .memoryLimit(dailyProblem.getMemoryLimit())
                .createdTime(dailyProblem.getCreatedTime())
                .build();
    }
    
    public static DailyProblemListResDto fromWithStats(DailyProblem dailyProblem, Long solverCount, 
                                                       Boolean hasSolved, Integer mySubmissionCount) {
        return DailyProblemListResDto.builder()
                .id(dailyProblem.getId())
                .problemDate(dailyProblem.getProblemDate())
                .title(dailyProblem.getTitle())
                .timeLimit(dailyProblem.getTimeLimit())
                .memoryLimit(dailyProblem.getMemoryLimit())
                .createdTime(dailyProblem.getCreatedTime())
                .solverCount(solverCount)
                .hasSolved(hasSolved)
                .mySubmissionCount(mySubmissionCount)
                .build();
    }
}
