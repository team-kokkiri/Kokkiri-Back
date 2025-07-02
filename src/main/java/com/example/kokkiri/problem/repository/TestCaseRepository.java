package com.example.kokkiri.problem.repository;

import com.example.kokkiri.problem.domain.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByDailyProblemIdOrderByOrderNum(Long problemId);
    List<TestCase> findByDailyProblemIdAndIsHiddenFalseOrderByOrderNum(Long problemId);
}
