package com.example.kokkiri.problem.dto;

import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SubmissionResultResDto {
    
    private boolean success;
    private String message;
    private ProblemSubmissionResDto submission;
    private DailyRankingResDto ranking;
    private boolean isAccepted;
    private boolean isNewRanking;
    private Integer totalTestCases;
    private Integer passedTestCases;
    private List<TestCaseResultDto> testCaseResults;
    
    public static SubmissionResultResDto from(DailyProblemFacadeService.SubmissionResult result) {
        return SubmissionResultResDto.builder()
                .success(result.success)
                .message(result.message)
                .submission(result.submission != null ? ProblemSubmissionResDto.from(result.submission) : null)
                .ranking(result.ranking != null ? DailyRankingResDto.from(result.ranking) : null)
                .isAccepted(result.submission != null && result.submission.getStatus().name().equals("ACCEPTED"))
                .isNewRanking(result.ranking != null)
                .totalTestCases(result.totalTestCases)
                .passedTestCases(result.passedTestCases)
                .testCaseResults(result.testCaseResults)
                .build();
    }
}
