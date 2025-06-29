package com.example.kokkiri.problem.dto;

import com.example.kokkiri.problem.domain.DailyProblem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class DailyProblemResDto {
    
    private Long id;
    private LocalDate problemDate;
    private String title;
    private String description;
    private String inputDescription;
    private String outputDescription;
    private String sampleInput;
    private String sampleOutput;
    private Integer timeLimit;
    private Integer memoryLimit;
    private String isActive;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    
    // 추가 정보 (필요시)
    private Long solverCount;
    private Boolean hasSolved;
    
    public static DailyProblemResDto from(DailyProblem dailyProblem) {
        return DailyProblemResDto.builder()
                .id(dailyProblem.getId())
                .problemDate(dailyProblem.getProblemDate())
                .title(dailyProblem.getTitle())
                .description(dailyProblem.getDescription())
                .inputDescription(dailyProblem.getInputDescription())
                .outputDescription(dailyProblem.getOutputDescription())
                .sampleInput(dailyProblem.getSampleInput())
                .sampleOutput(dailyProblem.getSampleOutput())
                .timeLimit(dailyProblem.getTimeLimit())
                .memoryLimit(dailyProblem.getMemoryLimit())
                .isActive(dailyProblem.getIsActive())
                .createdTime(dailyProblem.getCreatedTime())
                .updatedTime(dailyProblem.getUpdatedTime())
                .build();
    }
    
    public static DailyProblemResDto fromWithSolverInfo(DailyProblem dailyProblem, Long solverCount, Boolean hasSolved) {
        return DailyProblemResDto.builder()
                .id(dailyProblem.getId())
                .problemDate(dailyProblem.getProblemDate())
                .title(dailyProblem.getTitle())
                .description(dailyProblem.getDescription())
                .inputDescription(dailyProblem.getInputDescription())
                .outputDescription(dailyProblem.getOutputDescription())
                .sampleInput(dailyProblem.getSampleInput())
                .sampleOutput(dailyProblem.getSampleOutput())
                .timeLimit(dailyProblem.getTimeLimit())
                .memoryLimit(dailyProblem.getMemoryLimit())
                .isActive(dailyProblem.getIsActive())
                .createdTime(dailyProblem.getCreatedTime())
                .updatedTime(dailyProblem.getUpdatedTime())
                .solverCount(solverCount)
                .hasSolved(hasSolved)
                .build();
    }
}
