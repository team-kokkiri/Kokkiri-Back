package com.example.kokkiri.problem.repository;

import com.example.kokkiri.problem.domain.DailyRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRankingRepository extends JpaRepository<DailyRanking, Long> {
    
    // 특정 문제의 랭킹 조회 (순위순)
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.dailyProblem.id = :problemId ORDER BY dr.rankPosition ASC")
    List<DailyRanking> findByProblemOrderByRank(@Param("problemId") Long problemId);
    
    // 특정 문제의 랭킹 조회 (해결 시간순)
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.dailyProblem.id = :problemId ORDER BY dr.solveTime ASC")
    List<DailyRanking> findByProblemOrderBySolveTime(@Param("problemId") Long problemId);
    
    // 특정 날짜의 랭킹 조회
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.dailyProblem.problemDate = :problemDate ORDER BY dr.rankPosition ASC")
    List<DailyRanking> findByProblemDateOrderByRank(@Param("problemDate") LocalDate problemDate);
    
    // 오늘 문제의 랭킹 조회
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.dailyProblem.problemDate = CURRENT_DATE ORDER BY dr.rankPosition ASC")
    List<DailyRanking> findTodayRankingOrderByRank();
    
    // 특정 회원의 특정 문제 랭킹 조회
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.dailyProblem.id = :problemId AND dr.member.id = :memberId")
    Optional<DailyRanking> findByProblemAndMember(@Param("problemId") Long problemId, @Param("memberId") Long memberId);
    
    // 특정 회원의 모든 랭킹 기록 조회 (최신순)
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.member.id = :memberId ORDER BY dr.dailyProblem.problemDate DESC")
    List<DailyRanking> findByMemberOrderByProblemDateDesc(@Param("memberId") Long memberId);
    
    // 특정 문제에서 특정 회원이 이미 랭킹에 있는지 확인
    @Query("SELECT COUNT(dr) > 0 FROM DailyRanking dr WHERE dr.dailyProblem.id = :problemId AND dr.member.id = :memberId")
    boolean existsByProblemAndMember(@Param("problemId") Long problemId, @Param("memberId") Long memberId);
    
    // 특정 문제의 현재 최고 순위 조회 (다음 순위 계산용)
    @Query("SELECT COALESCE(MAX(dr.rankPosition), 0) FROM DailyRanking dr WHERE dr.dailyProblem.id = :problemId")
    int findMaxRankByProblem(@Param("problemId") Long problemId);
    
    // 특정 문제의 상위 N명 랭킹 조회
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.dailyProblem.id = :problemId ORDER BY dr.rankPosition ASC LIMIT :limit")
    List<DailyRanking> findTopRankingsByProblem(@Param("problemId") Long problemId, @Param("limit") int limit);
    
    // 최근 N일간의 랭킹 기록 조회
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.dailyProblem.problemDate >= :fromDate ORDER BY dr.dailyProblem.problemDate DESC, dr.rankPosition ASC")
    List<DailyRanking> findRecentRankings(@Param("fromDate") LocalDate fromDate);
    
    // 특정 회원의 최근 N일간 랭킹 기록
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.member.id = :memberId AND dr.dailyProblem.problemDate >= :fromDate ORDER BY dr.dailyProblem.problemDate DESC")
    List<DailyRanking> findMemberRecentRankings(@Param("memberId") Long memberId, @Param("fromDate") LocalDate fromDate);
    
    // 특정 시간 이후에 해결한 랭킹들 조회 (실시간 랭킹 업데이트용)
    @Query("SELECT dr FROM DailyRanking dr JOIN FETCH dr.dailyProblem JOIN FETCH dr.member m JOIN FETCH m.team WHERE dr.dailyProblem.id = :problemId AND dr.solveTime > :afterTime ORDER BY dr.solveTime ASC")
    List<DailyRanking> findSolvedAfterTime(@Param("problemId") Long problemId, @Param("afterTime") LocalDateTime afterTime);
}
