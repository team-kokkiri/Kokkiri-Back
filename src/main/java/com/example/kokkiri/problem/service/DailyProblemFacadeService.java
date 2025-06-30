package com.example.kokkiri.problem.service;

import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.domain.DailyRanking;
import com.example.kokkiri.problem.domain.ProblemSubmission;
import com.example.kokkiri.problem.domain.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * 일일 문제 시스템의 통합 서비스
 * 여러 서비스를 조합하여 복합적인 비즈니스 로직을 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DailyProblemFacadeService {
    
    private final DailyProblemService dailyProblemService;
    private final ProblemSubmissionService submissionService;
    private final DailyRankingService rankingService;
    
    /**
     * 오늘의 문제와 랭킹 정보를 함께 조회
     */
    public TodayProblemInfo getTodayProblemInfo(Long memberId) {
        Optional<DailyProblem> todayProblem = dailyProblemService.getTodayProblem();
        
        if (todayProblem.isEmpty()) {
            return new TodayProblemInfo(null, List.of(), false, 0);
        }
        
        DailyProblem problem = todayProblem.get();
        List<DailyRanking> rankings = rankingService.getTodayRanking();
        boolean hasSolved = memberId != null && rankingService.hasSolved(problem.getId(), memberId);
        int submissionCount = memberId != null ? submissionService.getSubmissionCount(problem.getId(), memberId) : 0;
        
        return new TodayProblemInfo(problem, rankings, hasSolved, submissionCount);
    }
    
    /**
     * 코드 제출 및 전체 처리 (제출 → 채점 → 랭킹 업데이트)
     */
    @Transactional
    public Mono<SubmissionResult> submitAndProcess(Long problemId, Long memberId, String sourceCode, String language) {
        // 이미 해결한 문제인지 확인
        if (rankingService.hasSolved(problemId, memberId)) {
            log.warn("이미 해결한 문제에 재제출 시도: 문제ID={}, 회원ID={}", problemId, memberId);
            return Mono.just(new SubmissionResult(false, "이미 해결한 문제입니다.", null, null));
        }
        
        // 오늘 문제인지 확인
        Optional<DailyProblem> todayProblem = dailyProblemService.getTodayProblem();
        if (todayProblem.isEmpty() || !todayProblem.get().getId().equals(problemId)) {
            return Mono.just(new SubmissionResult(false, "오늘 출제된 문제가 아닙니다.", null, null));
        }
        
        // 코드 제출 및 채점 처리
        return submissionService.submitCode(problemId, memberId, sourceCode, language)
                .map(submission -> {
                    DailyRanking ranking = null;
                    
                    // 정답인 경우 랭킹 정보도 함께 반환
                    if (submission.getStatus() == SubmissionStatus.ACCEPTED) {
                        try {
                            ranking = rankingService.getMemberRanking(problemId, memberId).orElse(null);
                        } catch (Exception e) {
                            log.error("랭킹 조회 실패: 문제ID={}, 회원ID={}", problemId, memberId, e);
                        }
                    }
                    
                    return new SubmissionResult(true, "제출이 완료되었습니다.", submission, ranking);
                })
                .onErrorReturn(new SubmissionResult(false, "제출 처리 중 오류가 발생했습니다.", null, null));
    }
    
    // DTO 클래스들
    
    public static class TodayProblemInfo {
        public final DailyProblem problem;
        public final List<DailyRanking> rankings;
        public final boolean hasSolved;
        public final int submissionCount;
        
        public TodayProblemInfo(DailyProblem problem, List<DailyRanking> rankings, boolean hasSolved, int submissionCount) {
            this.problem = problem;
            this.rankings = rankings;
            this.hasSolved = hasSolved;
            this.submissionCount = submissionCount;
        }
    }
    
    public static class SubmissionResult {
        public final boolean success;
        public final String message;
        public final ProblemSubmission submission;
        public final DailyRanking ranking;
        
        public SubmissionResult(boolean success, String message, ProblemSubmission submission, DailyRanking ranking) {
            this.success = success;
            this.message = message;
            this.submission = submission;
            this.ranking = ranking;
        }
    }
}
