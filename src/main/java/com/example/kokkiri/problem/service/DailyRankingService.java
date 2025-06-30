package com.example.kokkiri.problem.service;

import com.example.kokkiri.problem.domain.DailyRanking;
import com.example.kokkiri.problem.domain.ProblemSubmission;
import com.example.kokkiri.problem.repository.DailyRankingRepository;
import com.example.kokkiri.problem.repository.ProblemSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DailyRankingService {
    
    private final DailyRankingRepository rankingRepository;
    private final ProblemSubmissionRepository submissionRepository;
    
    /**
     * 정답 제출 시 랭킹 업데이트
     */
    @Transactional
    public DailyRanking updateRanking(ProblemSubmission submission) {
        Long problemId = submission.getDailyProblem().getId();
        Long memberId = submission.getMember().getId();
        
        // 이미 랭킹에 있는지 확인 (한 문제당 한 번만 랭킹 기록)
        if (rankingRepository.existsByProblemAndMember(problemId, memberId)) {
            log.warn("이미 랭킹에 등록된 회원입니다: 문제ID={}, 회원ID={}", problemId, memberId);
            return rankingRepository.findByProblemAndMember(problemId, memberId).orElse(null);
        }
        
        // 현재 최고 순위 조회하여 다음 순위 계산
        int nextRank = rankingRepository.findMaxRankByProblem(problemId) + 1;
        
        // 해당 회원의 제출 횟수 계산 (정답까지의 시도 횟수)
        int submissionCount = submissionRepository.countSubmissionsByProblemAndMember(problemId, memberId);
        
        // 랭킹 기록 생성
        DailyRanking ranking = DailyRanking.builder()
                .dailyProblem(submission.getDailyProblem())
                .member(submission.getMember())
                .submission(submission)
                .rankPosition(nextRank)
                .solveTime(submission.getJudgeTime() != null ? submission.getJudgeTime() : LocalDateTime.now())
                .submissionCount(submissionCount)
                .executionTime(submission.getExecutionTime())
                .build();
        
        DailyRanking savedRanking = rankingRepository.save(ranking);
        log.info("새로운 랭킹 기록 생성: 문제ID={}, 회원ID={}, 순위={}", problemId, memberId, nextRank);
        
        return savedRanking;
    }
    
    /**
     * 특정 문제의 랭킹 조회 (순위순)
     */
    public List<DailyRanking> getProblemRanking(Long problemId) {
        return rankingRepository.findByProblemOrderByRank(problemId);
    }
    
    /**
     * 특정 문제의 랭킹 조회 (해결 시간순)
     */
    public List<DailyRanking> getProblemRankingBySolveTime(Long problemId) {
        return rankingRepository.findByProblemOrderBySolveTime(problemId);
    }
    
    /**
     * 오늘 문제의 랭킹 조회
     */
    public List<DailyRanking> getTodayRanking() {
        return rankingRepository.findTodayRankingOrderByRank();
    }
    
    /**
     * 특정 날짜의 랭킹 조회
     */
    public List<DailyRanking> getRankingByDate(LocalDate date) {
        return rankingRepository.findByProblemDateOrderByRank(date);
    }
    
    /**
     * 특정 회원의 특정 문제 랭킹 조회
     */
    public Optional<DailyRanking> getMemberRanking(Long problemId, Long memberId) {
        return rankingRepository.findByProblemAndMember(problemId, memberId);
    }
    
    /**
     * 특정 회원의 모든 랭킹 기록 조회 (최신순)
     */
    public List<DailyRanking> getMemberAllRankings(Long memberId) {
        return rankingRepository.findByMemberOrderByProblemDateDesc(memberId);
    }
    
    /**
     * 특정 회원의 최근 N일간 랭킹 기록
     */
    public List<DailyRanking> getMemberRecentRankings(Long memberId, int days) {
        LocalDate fromDate = LocalDate.now().minusDays(days - 1);
        return rankingRepository.findMemberRecentRankings(memberId, fromDate);
    }
    
    /**
     * 특정 문제의 상위 N명 랭킹 조회
     */
    public List<DailyRanking> getTopRankings(Long problemId, int limit) {
        return rankingRepository.findTopRankingsByProblem(problemId, limit);
    }
    
    /**
     * 최근 N일간의 모든 랭킹 기록
     */
    public List<DailyRanking> getRecentRankings(int days) {
        LocalDate fromDate = LocalDate.now().minusDays(days - 1);
        return rankingRepository.findRecentRankings(fromDate);
    }
    
    /**
     * 특정 회원이 특정 문제를 해결했는지 확인
     */
    public boolean hasSolved(Long problemId, Long memberId) {
        return rankingRepository.existsByProblemAndMember(problemId, memberId);
    }
    
    /**
     * 특정 문제의 해결자 수
     */
    public long getSolverCount(Long problemId) {
        return rankingRepository.findByProblemOrderByRank(problemId).size();
    }
    
    /**
     * 특정 회원의 전체 해결 문제 수
     */
    public long getMemberSolvedCount(Long memberId) {
        return rankingRepository.findByMemberOrderByProblemDateDesc(memberId).size();
    }
    
    /**
     * 랭킹 기록 삭제 (관리자용)
     */
    @Transactional
    public void deleteRanking(Long rankingId) {
        DailyRanking ranking = rankingRepository.findById(rankingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랭킹입니다: " + rankingId));
        
        Long problemId = ranking.getDailyProblem().getId();
        int deletedRank = ranking.getRankPosition();
        
        // 랭킹 삭제
        rankingRepository.delete(ranking);
        log.info("랭킹 기록 삭제: 랭킹ID={}, 문제ID={}, 순위={}", rankingId, problemId, deletedRank);
        
        // 삭제된 순위보다 낮은 순위들을 한 단계씩 올림
        reorderRankingsAfterDeletion(problemId, deletedRank);
    }
    
    /**
     * 랭킹 삭제 후 순위 재정렬
     */
    @Transactional
    public void reorderRankingsAfterDeletion(Long problemId, int deletedRank) {
        // 삭제된 순위보다 낮은 순위들을 조회하여 한 단계씩 올림
        List<DailyRanking> rankingsToUpdate = rankingRepository.findByProblemOrderByRank(problemId)
                .stream()
                .filter(ranking -> ranking.getRankPosition() > deletedRank)
                .toList();
        
        for (DailyRanking ranking : rankingsToUpdate) {
            ranking.setRankPosition(ranking.getRankPosition() - 1);
            rankingRepository.save(ranking);
        }
        
        log.info("랭킹 재정렬 완료: 문제ID={}, 업데이트된 랭킹 수={}", problemId, rankingsToUpdate.size());
    }
    
    /**
     * 특정 시간 이후 해결한 사람들 조회 (실시간 랭킹용)
     */
    public List<DailyRanking> getSolvedAfterTime(Long problemId, LocalDateTime afterTime) {
        return rankingRepository.findSolvedAfterTime(problemId, afterTime);
    }
    
    /**
     * 랭킹 통계 정보 조회
     */
    public RankingStats getRankingStats(Long problemId) {
        List<DailyRanking> rankings = rankingRepository.findByProblemOrderByRank(problemId);
        
        if (rankings.isEmpty()) {
            return new RankingStats(0, 0.0, 0, 0);
        }
        
        int totalSolvers = rankings.size();
        double avgSubmissionCount = rankings.stream()
                .mapToInt(DailyRanking::getSubmissionCount)
                .average()
                .orElse(0.0);
        
        int fastestTime = rankings.stream()
                .mapToInt(ranking -> ranking.getExecutionTime() != null ? ranking.getExecutionTime() : 0)
                .filter(time -> time > 0)
                .min()
                .orElse(0);
        
        int avgExecutionTime = (int) rankings.stream()
                .mapToInt(ranking -> ranking.getExecutionTime() != null ? ranking.getExecutionTime() : 0)
                .filter(time -> time > 0)
                .average()
                .orElse(0.0);
        
        return new RankingStats(totalSolvers, avgSubmissionCount, fastestTime, avgExecutionTime);
    }
    
    /**
     * 랭킹 통계 정보를 담는 내부 클래스
     */
    public static class RankingStats {
        public final int totalSolvers;
        public final double avgSubmissionCount;
        public final int fastestExecutionTime;
        public final int avgExecutionTime;
        
        public RankingStats(int totalSolvers, double avgSubmissionCount, int fastestExecutionTime, int avgExecutionTime) {
            this.totalSolvers = totalSolvers;
            this.avgSubmissionCount = avgSubmissionCount;
            this.fastestExecutionTime = fastestExecutionTime;
            this.avgExecutionTime = avgExecutionTime;
        }
    }
}
