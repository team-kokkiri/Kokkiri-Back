package com.example.kokkiri.problem.dto;

import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ProblemSummaryResDto {
    
    private Long id;
    private LocalDate problemDate;
    private String title;
    private Integer timeLimit;
    private Integer memoryLimit;
    private boolean solved;
    private long solverCount;
    
    public static ProblemSummaryResDto from(DailyProblemFacadeService.ProblemSummary summary) {
        return ProblemSummaryResDto.builder()
                .id(summary.problem.getId())
                .problemDate(summary.problem.getProblemDate())
                .title(summary.problem.getTitle())
                .timeLimit(summary.problem.getTimeLimit())
                .memoryLimit(summary.problem.getMemoryLimit())
                .solved(summary.solved)
                .solverCount(summary.solverCount)
                .build();
    }
}
