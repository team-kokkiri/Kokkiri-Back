package com.example.kokkiri.problem.dto;

import com.example.kokkiri.problem.domain.DailyRanking;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class DailyRankingResDto {
    
    private Long id;
    private Long problemId;
    private String problemTitle;
    private LocalDate problemDate;
    private Long memberId;
    private String memberNickname;
    private String memberAvatar;
    private Long memberTeamId;      // 멤버 팀 ID 추가
    private String memberTeamName;   // 멤버 팀 이름 추가
    private Integer rankPosition;
    private LocalDateTime solveTime;
    private Integer submissionCount;
    private Integer executionTime;
    private LocalDateTime createdTime;
    private Long submissionId; // 해결한 제출 ID
    
    public static DailyRankingResDto from(DailyRanking ranking) {
        return DailyRankingResDto.builder()
                .id(ranking.getId())
                .problemId(ranking.getDailyProblem().getId())
                .problemTitle(ranking.getDailyProblem().getTitle())
                .problemDate(ranking.getDailyProblem().getProblemDate())
                .memberId(ranking.getMember().getId())
                .memberNickname(ranking.getMember().getNickname())
                .memberAvatar(ranking.getMember().getAvatar())
                .memberTeamId(ranking.getMember().getTeam().getId())        // 팀 ID 추가
                .memberTeamName(ranking.getMember().getTeam().getTeamName()) // 팀 이름 추가
                .rankPosition(ranking.getRankPosition())
                .solveTime(ranking.getSolveTime())
                .submissionCount(ranking.getSubmissionCount())
                .executionTime(ranking.getExecutionTime())
                .createdTime(ranking.getCreatedTime())
                .submissionId(ranking.getSubmission().getId())
                .build();
    }
}
