package com.example.kokkiri.problem.dto;

import com.example.kokkiri.problem.domain.ProblemSubmission;
import com.example.kokkiri.problem.domain.SubmissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProblemSubmissionResDto {
    
    private Long id;
    private Long problemId;
    private String problemTitle;
    private Long memberId;
    private String memberNickname;
    private String sourceCode;
    private String language;
    private SubmissionStatus status;
    private Integer executionTime;
    private Integer memoryUsage;
    private String judgeResult;
    private String errorMessage;
    private LocalDateTime submissionTime;
    private LocalDateTime judgeTime;
    
    public static ProblemSubmissionResDto from(ProblemSubmission submission) {
        return ProblemSubmissionResDto.builder()
                .id(submission.getId())
                .problemId(submission.getDailyProblem().getId())
                .problemTitle(submission.getDailyProblem().getTitle())
                .memberId(submission.getMember().getId())
                .memberNickname(submission.getMember().getNickname())
                .sourceCode(submission.getSourceCode())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .executionTime(submission.getExecutionTime())
                .memoryUsage(submission.getMemoryUsage())
                .judgeResult(submission.getJudgeResult())
                .errorMessage(submission.getErrorMessage())
                .submissionTime(submission.getSubmissionTime())
                .judgeTime(submission.getJudgeTime())
                .build();
    }
    
    // 소스코드를 제외한 버전 (목록 조회용)
    public static ProblemSubmissionResDto fromWithoutSourceCode(ProblemSubmission submission) {
        return ProblemSubmissionResDto.builder()
                .id(submission.getId())
                .problemId(submission.getDailyProblem().getId())
                .problemTitle(submission.getDailyProblem().getTitle())
                .memberId(submission.getMember().getId())
                .memberNickname(submission.getMember().getNickname())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .executionTime(submission.getExecutionTime())
                .memoryUsage(submission.getMemoryUsage())
                .errorMessage(submission.getErrorMessage())
                .submissionTime(submission.getSubmissionTime())
                .judgeTime(submission.getJudgeTime())
                .build();
    }
}
