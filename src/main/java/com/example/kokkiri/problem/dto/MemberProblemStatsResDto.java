package com.example.kokkiri.problem.dto;

import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberProblemStatsResDto {
    
    private long totalSolved;
    private long totalSubmissions;
    private int bestRank;
    private double avgAttempts;
    private long recentSolved;
    private double successRate;
    
    public static MemberProblemStatsResDto from(DailyProblemFacadeService.MemberProblemStats stats) {
        double successRate = stats.totalSubmissions > 0 ? 
                (double) stats.totalSolved / stats.totalSubmissions * 100 : 0.0;
        
        return MemberProblemStatsResDto.builder()
                .totalSolved(stats.totalSolved)
                .totalSubmissions(stats.totalSubmissions)
                .bestRank(stats.bestRank)
                .avgAttempts(stats.avgAttempts)
                .recentSolved(stats.recentSolved)
                .successRate(Math.round(successRate * 100.0) / 100.0) // 소수점 2자리
                .build();
    }
}
