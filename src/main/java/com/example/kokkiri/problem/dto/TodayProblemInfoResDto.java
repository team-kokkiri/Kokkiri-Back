package com.example.kokkiri.problem.dto;

import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TodayProblemInfoResDto {
    
    private DailyProblemResDto problem;
    private List<DailyRankingResDto> rankings;
    private boolean hasSolved;
    private int submissionCount;
    private boolean hasProblemsToday;
    
    public static TodayProblemInfoResDto from(DailyProblemFacadeService.TodayProblemInfo info) {
        return TodayProblemInfoResDto.builder()
                .problem(info.problem != null ? DailyProblemResDto.from(info.problem) : null)
                .rankings(info.rankings.stream()
                        .map(DailyRankingResDto::from)
                        .toList())
                .hasSolved(info.hasSolved)
                .submissionCount(info.submissionCount)
                .hasProblemsToday(info.problem != null)
                .build();
    }
}
