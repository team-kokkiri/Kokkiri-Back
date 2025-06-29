package com.example.kokkiri.problem.dto;

import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProblemDetailInfoResDto {
    
    private DailyProblemResDto problem;
    private List<DailyRankingResDto> rankings;
    private List<ProblemSubmissionResDto> mySubmissions;
    private boolean hasSolved;
    private int submissionCount;
    private boolean problemExists;
    
    public static ProblemDetailInfoResDto from(DailyProblemFacadeService.ProblemDetailInfo info) {
        return ProblemDetailInfoResDto.builder()
                .problem(info.problem != null ? DailyProblemResDto.from(info.problem) : null)
                .rankings(info.rankings.stream()
                        .map(DailyRankingResDto::from)
                        .toList())
                .mySubmissions(info.memberSubmissions.stream()
                        .map(ProblemSubmissionResDto::fromWithoutSourceCode)
                        .toList())
                .hasSolved(info.hasSolved)
                .submissionCount(info.submissionCount)
                .problemExists(info.problem != null)
                .build();
    }
}
