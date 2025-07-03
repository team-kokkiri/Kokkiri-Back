package com.example.kokkiri.problem.repository;

import com.example.kokkiri.problem.domain.DailyProblem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    // 특정 날짜의 문제 조회
    Optional<DailyProblem> findByProblemDateAndIsActive(LocalDate problemDate, String isActive);
    
    // 활성화된 모든 문제 페이징 조회
    Page<DailyProblem> findByIsActive(String isActive, Pageable pageable);
    
    // 특정 문제의 해결자 수 조회
    @Query("SELECT COUNT(DISTINCT ps.member.id) FROM ProblemSubmission ps WHERE ps.dailyProblem.id = :problemId AND ps.status = 'ACCEPTED'")
    Long countSolversByProblemId(@Param("problemId") Long problemId);
    
    // 특정 사용자가 특정 문제를 해결했는지 확인
    @Query("SELECT COUNT(ps) > 0 FROM ProblemSubmission ps WHERE ps.dailyProblem.id = :problemId AND ps.member.id = :memberId AND ps.status = 'ACCEPTED'")
    Boolean hasUserSolvedProblem(@Param("problemId") Long problemId, @Param("memberId") Long memberId);
}
