package com.example.kokkiri.problem.controller;

import com.example.kokkiri.common.dto.CommonResDto;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.problem.dto.DailyRankingResDto;
import com.example.kokkiri.problem.dto.MemberProblemStatsResDto;
import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import com.example.kokkiri.problem.service.DailyRankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Daily Ranking", description = "일일 문제 랭킹 API")
public class DailyRankingController {
    
    private final DailyRankingService rankingService;
    private final DailyProblemFacadeService problemFacadeService;
    
    /**
     * 오늘 문제의 랭킹 조회
     */
    @GetMapping("/today")
    @Operation(summary = "오늘 문제 랭킹 조회", description = "오늘 문제의 전체 랭킹을 조회합니다.")
    public ResponseEntity<CommonResDto> getTodayRanking() {
        try {
            List<DailyRankingResDto> rankings = rankingService.getTodayRanking()
                    .stream()
                    .map(DailyRankingResDto::from)
                    .toList();
            
            log.info("오늘 문제 랭킹 조회 완료: {} 건", rankings.size());
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "오늘 문제 랭킹 조회 성공", rankings));
            
        } catch (Exception e) {
            log.error("오늘 문제 랭킹 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 특정 문제의 랭킹 조회
     */
    @GetMapping("/problem/{problemId}")
    @Operation(summary = "특정 문제 랭킹 조회", description = "특정 문제의 전체 랭킹을 조회합니다.")
    public ResponseEntity<CommonResDto> getProblemRanking(
            @Parameter(description = "문제 ID", required = true) @PathVariable Long problemId,
            @Parameter(description = "정렬 기준 (rank: 순위순, time: 해결시간순)", example = "rank") 
            @RequestParam(defaultValue = "rank") String sortBy) {
        
        try {
            List<DailyRankingResDto> rankings;
            
            if ("time".equals(sortBy)) {
                rankings = rankingService.getProblemRankingBySolveTime(problemId)
                        .stream()
                        .map(DailyRankingResDto::from)
                        .toList();
            } else {
                rankings = rankingService.getProblemRanking(problemId)
                        .stream()
                        .map(DailyRankingResDto::from)
                        .toList();
            }
            
            log.info("문제 {} 랭킹 조회 완료: {} 건 (정렬: {})", problemId, rankings.size(), sortBy);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "문제 랭킹 조회 성공", rankings));
            
        } catch (Exception e) {
            log.error("문제 {} 랭킹 조회 실패", problemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 특정 문제의 상위 N명 랭킹 조회
     */
    @GetMapping("/problem/{problemId}/top")
    @Operation(summary = "특정 문제 상위 랭킹 조회", description = "특정 문제의 상위 N명 랭킹을 조회합니다.")
    public ResponseEntity<CommonResDto> getTopRankings(
            @Parameter(description = "문제 ID", required = true) @PathVariable Long problemId,
            @Parameter(description = "조회할 랭킹 수", example = "10") 
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            if (limit <= 0 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(new CommonResDto(HttpStatus.BAD_REQUEST, "조회할 랭킹 수는 1~100 사이여야 합니다.", null));
            }
            
            List<DailyRankingResDto> rankings = rankingService.getTopRankings(problemId, limit)
                    .stream()
                    .map(DailyRankingResDto::from)
                    .toList();
            
            log.info("문제 {} 상위 {} 랭킹 조회 완료", problemId, limit);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "상위 랭킹 조회 성공", rankings));
            
        } catch (Exception e) {
            log.error("문제 {} 상위 랭킹 조회 실패", problemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 특정 날짜의 랭킹 조회
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "특정 날짜 랭킹 조회", description = "특정 날짜 문제의 전체 랭킹을 조회합니다.")
    public ResponseEntity<CommonResDto> getRankingByDate(
            @Parameter(description = "조회할 날짜 (YYYY-MM-DD)", required = true, example = "2025-06-29") 
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            List<DailyRankingResDto> rankings = rankingService.getRankingByDate(date)
                    .stream()
                    .map(DailyRankingResDto::from)
                    .toList();
            
            log.info("{} 날짜 랭킹 조회 완료: {} 건", date, rankings.size());
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "날짜별 랭킹 조회 성공", rankings));
            
        } catch (Exception e) {
            log.error("{} 날짜 랭킹 조회 실패", date, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 최근 N일간의 랭킹 조회
     */
    @GetMapping("/recent")
    @Operation(summary = "최근 랭킹 조회", description = "최근 N일간의 모든 랭킹을 조회합니다.")
    public ResponseEntity<CommonResDto> getRecentRankings(
            @Parameter(description = "조회할 일수", example = "7") 
            @RequestParam(defaultValue = "7") int days) {
        
        try {
            if (days <= 0 || days > 30) {
                return ResponseEntity.badRequest()
                        .body(new CommonResDto(HttpStatus.BAD_REQUEST, "조회할 일수는 1~30 사이여야 합니다.", null));
            }
            
            List<DailyRankingResDto> rankings = rankingService.getRecentRankings(days)
                    .stream()
                    .map(DailyRankingResDto::from)
                    .toList();
            
            log.info("최근 {} 일간 랭킹 조회 완료: {} 건", days, rankings.size());
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "최근 랭킹 조회 성공", rankings));
            
        } catch (Exception e) {
            log.error("최근 {} 일간 랭킹 조회 실패", days, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 특정 회원의 랭킹 기록 조회
     */
    @GetMapping("/member/{memberId}")
    @Operation(summary = "회원별 랭킹 기록 조회", description = "특정 회원의 모든 랭킹 기록을 조회합니다.")
    public ResponseEntity<CommonResDto> getMemberRankings(
            @Parameter(description = "회원 ID", required = true) @PathVariable Long memberId,
            @Parameter(description = "조회할 일수 (0이면 전체)", example = "30") 
            @RequestParam(defaultValue = "0") int days) {
        
        try {
            List<DailyRankingResDto> rankings;
            
            if (days > 0) {
                if (days > 365) {
                    return ResponseEntity.badRequest()
                            .body(new CommonResDto(HttpStatus.BAD_REQUEST, "조회할 일수는 365일 이하여야 합니다.", null));
                }
                rankings = rankingService.getMemberRecentRankings(memberId, days)
                        .stream()
                        .map(DailyRankingResDto::from)
                        .toList();
            } else {
                rankings = rankingService.getMemberAllRankings(memberId)
                        .stream()
                        .map(DailyRankingResDto::from)
                        .toList();
            }
            
            log.info("회원 {} 랭킹 기록 조회 완료: {} 건 (최근 {} 일)", memberId, rankings.size(), days);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "회원 랭킹 조회 성공", rankings));
            
        } catch (Exception e) {
            log.error("회원 {} 랭킹 조회 실패", memberId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 내 랭킹 기록 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 랭킹 기록 조회", description = "로그인한 회원의 랭킹 기록을 조회합니다.")
    public ResponseEntity<CommonResDto> getMyRankings(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "조회할 일수 (0이면 전체)", example = "30") 
            @RequestParam(defaultValue = "0") int days) {
        
        try {
            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null));
            }
            
            List<DailyRankingResDto> rankings;
            
            if (days > 0) {
                if (days > 365) {
                    return ResponseEntity.badRequest()
                            .body(new CommonResDto(HttpStatus.BAD_REQUEST, "조회할 일수는 365일 이하여야 합니다.", null));
                }
                rankings = rankingService.getMemberRecentRankings(member.getId(), days)
                        .stream()
                        .map(DailyRankingResDto::from)
                        .toList();
            } else {
                rankings = rankingService.getMemberAllRankings(member.getId())
                        .stream()
                        .map(DailyRankingResDto::from)
                        .toList();
            }
            
            log.info("회원 {} 개인 랭킹 조회 완료: {} 건", member.getNickname(), rankings.size());
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "내 랭킹 조회 성공", rankings));
            
        } catch (Exception e) {
            log.error("개인 랭킹 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 특정 문제에서의 내 랭킹 조회
     */
    @GetMapping("/problem/{problemId}/my")
    @Operation(summary = "문제별 내 랭킹 조회", description = "특정 문제에서의 내 랭킹을 조회합니다.")
    public ResponseEntity<CommonResDto> getMyRankingInProblem(
            @Parameter(description = "문제 ID", required = true) @PathVariable Long problemId,
            @AuthenticationPrincipal Member member) {
        
        try {
            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null));
            }
            
            return rankingService.getMemberRanking(problemId, member.getId())
                    .map(ranking -> {
                        DailyRankingResDto rankingDto = DailyRankingResDto.from(ranking);
                        log.info("회원 {} 문제 {} 랭킹 조회 성공: {}위", member.getNickname(), problemId, ranking.getRankPosition());
                        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "내 랭킹 조회 성공", rankingDto));
                    })
                    .orElse(ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "해당 문제에서 아직 해결하지 못했습니다.", null)));
            
        } catch (Exception e) {
            log.error("회원 {} 문제 {} 랭킹 조회 실패", member != null ? member.getId() : "unknown", problemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 특정 문제의 랭킹 통계 조회
     */
    @GetMapping("/problem/{problemId}/stats")
    @Operation(summary = "문제별 랭킹 통계", description = "특정 문제의 랭킹 통계 정보를 조회합니다.")
    public ResponseEntity<CommonResDto> getProblemRankingStats(
            @Parameter(description = "문제 ID", required = true) @PathVariable Long problemId) {
        
        try {
            DailyRankingService.RankingStats stats = rankingService.getRankingStats(problemId);
            
            Map<String, Object> statsData = Map.of(
                    "totalSolvers", stats.totalSolvers,
                    "avgSubmissionCount", Math.round(stats.avgSubmissionCount * 100.0) / 100.0,
                    "fastestExecutionTime", stats.fastestExecutionTime,
                    "avgExecutionTime", stats.avgExecutionTime
            );
            
            log.info("문제 {} 랭킹 통계 조회 완료", problemId);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랭킹 통계 조회 성공", statsData));
            
        } catch (Exception e) {
            log.error("문제 {} 랭킹 통계 조회 실패", problemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "통계 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 회원의 문제 해결 통계 조회
     */
    @GetMapping("/member/{memberId}/stats")
    @Operation(summary = "회원별 문제 해결 통계", description = "특정 회원의 문제 해결 통계를 조회합니다.")
    public ResponseEntity<CommonResDto> getMemberProblemStats(
            @Parameter(description = "회원 ID", required = true) @PathVariable Long memberId) {
        
        try {
            MemberProblemStatsResDto stats = problemFacadeService.getMemberProblemStats(memberId);
            
            log.info("회원 {} 문제 해결 통계 조회 완료", memberId);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "회원 통계 조회 성공", stats));
            
        } catch (Exception e) {
            log.error("회원 {} 통계 조회 실패", memberId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "통계 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 내 문제 해결 통계 조회
     */
    @GetMapping("/my/stats")
    @Operation(summary = "내 문제 해결 통계", description = "로그인한 회원의 문제 해결 통계를 조회합니다.")
    public ResponseEntity<CommonResDto> getMyProblemStats(@AuthenticationPrincipal Member member) {
        
        try {
            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null));
            }
            
            MemberProblemStatsResDto stats = problemFacadeService.getMemberProblemStats(member.getId());
            
            log.info("회원 {} 개인 통계 조회 완료", member.getNickname());
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "내 통계 조회 성공", stats));
            
        } catch (Exception e) {
            log.error("개인 통계 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "통계 조회 중 오류가 발생했습니다.", null));
        }
    }
    
    /**
     * 특정 문제의 해결 여부 확인
     */
    @GetMapping("/problem/{problemId}/solved")
    @Operation(summary = "문제 해결 여부 확인", description = "내가 특정 문제를 해결했는지 확인합니다.")
    public ResponseEntity<CommonResDto> checkProblemSolved(
            @Parameter(description = "문제 ID", required = true) @PathVariable Long problemId,
            @AuthenticationPrincipal Member member) {
        
        try {
            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null));
            }
            
            boolean hasSolved = rankingService.hasSolved(problemId, member.getId());
            
            Map<String, Object> result = Map.of(
                    "problemId", problemId,
                    "memberId", member.getId(),
                    "hasSolved", hasSolved
            );
            
            log.info("회원 {} 문제 {} 해결 여부: {}", member.getNickname(), problemId, hasSolved);
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "해결 여부 확인 성공", result));
            
        } catch (Exception e) {
            log.error("문제 해결 여부 확인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "해결 여부 확인 중 오류가 발생했습니다.", null));
        }
    }
}
