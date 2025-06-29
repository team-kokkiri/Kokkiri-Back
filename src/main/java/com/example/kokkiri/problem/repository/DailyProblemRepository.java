package com.example.kokkiri.problem.repository;

import com.example.kokkiri.problem.domain.DailyProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyProblemRepository extends JpaRepository<DailyProblem, Long> {
    
    // 오늘 문제 조회
    @Query("SELECT dp FROM DailyProblem dp WHERE dp.problemDate = CURRENT_DATE AND dp.isActive = 'Y'")
    Optional<DailyProblem> findTodayProblem();
    
    // 해당 날짜에 문제가 있는지 확인
    boolean existsByProblemDate(LocalDate problemDate);
}
