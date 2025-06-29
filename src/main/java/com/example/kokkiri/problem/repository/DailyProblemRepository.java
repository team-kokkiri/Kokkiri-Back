package com.example.kokkiri.problem.repository;

import com.example.kokkiri.problem.domain.DailyProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyProblemRepository extends JpaRepository<DailyProblem, Long> {
    
    // 날짜로 문제 조회
    Optional<DailyProblem> findByProblemDate(LocalDate problemDate);
    
    // 오늘 문제 조회
    @Query("SELECT dp FROM DailyProblem dp WHERE dp.problemDate = CURRENT_DATE AND dp.isActive = 'Y'")
    Optional<DailyProblem> findTodayProblem();
    
    // 활성화된 문제만 조회 (최신순)
    @Query("SELECT dp FROM DailyProblem dp WHERE dp.isActive = 'Y' ORDER BY dp.problemDate DESC")
    List<DailyProblem> findActiveProblemsOrderByDateDesc();
    
    // 날짜 범위로 문제 조회
    @Query("SELECT dp FROM DailyProblem dp WHERE dp.problemDate BETWEEN :startDate AND :endDate AND dp.isActive = 'Y' ORDER BY dp.problemDate DESC")
    List<DailyProblem> findByProblemDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // 해당 날짜에 문제가 있는지 확인
    boolean existsByProblemDate(LocalDate problemDate);
    
    // 최근 n일 문제 조회
    @Query("SELECT dp FROM DailyProblem dp WHERE dp.problemDate >= :fromDate AND dp.isActive = 'Y' ORDER BY dp.problemDate DESC")
    List<DailyProblem> findRecentProblems(@Param("fromDate") LocalDate fromDate);
}
