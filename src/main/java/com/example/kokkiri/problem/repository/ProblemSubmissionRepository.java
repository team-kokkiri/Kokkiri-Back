package com.example.kokkiri.problem.repository;

import com.example.kokkiri.problem.domain.ProblemSubmission;
import com.example.kokkiri.problem.domain.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemSubmissionRepository extends JpaRepository<ProblemSubmission, Long> {
    
    // 특정 문제에 대한 특정 회원의 모든 제출 기록 조회 (최신순)
    @Query("SELECT ps FROM ProblemSubmission ps WHERE ps.dailyProblem.id = :problemId AND ps.member.id = :memberId ORDER BY ps.submissionTime DESC")
    List<ProblemSubmission> findByProblemAndMemberOrderBySubmissionTimeDesc(@Param("problemId") Long problemId, @Param("memberId") Long memberId);
    
    // 특정 문제에 대한 특정 회원의 정답 제출 기록 조회
    @Query("SELECT ps FROM ProblemSubmission ps WHERE ps.dailyProblem.id = :problemId AND ps.member.id = :memberId AND ps.status = 'ACCEPTED' ORDER BY ps.submissionTime ASC")
    Optional<ProblemSubmission> findAcceptedSubmission(@Param("problemId") Long problemId, @Param("memberId") Long memberId);
    
    // 특정 문제에 대한 모든 제출 기록 조회 (최신순)
    @Query("SELECT ps FROM ProblemSubmission ps WHERE ps.dailyProblem.id = :problemId ORDER BY ps.submissionTime DESC")
    List<ProblemSubmission> findByProblemOrderBySubmissionTimeDesc(@Param("problemId") Long problemId);
    
    // 특정 회원의 모든 제출 기록 조회 (최신순)
    @Query("SELECT ps FROM ProblemSubmission ps WHERE ps.member.id = :memberId ORDER BY ps.submissionTime DESC")
    List<ProblemSubmission> findByMemberOrderBySubmissionTimeDesc(@Param("memberId") Long memberId);
    
    // 특정 문제에 대한 정답 제출 기록들 (시간순)
    @Query("SELECT ps FROM ProblemSubmission ps WHERE ps.dailyProblem.id = :problemId AND ps.status = 'ACCEPTED' ORDER BY ps.submissionTime ASC")
    List<ProblemSubmission> findAcceptedSubmissionsByProblem(@Param("problemId") Long problemId);
    
    // 특정 회원이 특정 문제를 이미 정답처리 했는지 확인
    @Query("SELECT COUNT(ps) > 0 FROM ProblemSubmission ps WHERE ps.dailyProblem.id = :problemId AND ps.member.id = :memberId AND ps.status = 'ACCEPTED'")
    boolean existsAcceptedSubmission(@Param("problemId") Long problemId, @Param("memberId") Long memberId);
    
    // 특정 문제에 대한 특정 회원의 제출 횟수
    @Query("SELECT COUNT(ps) FROM ProblemSubmission ps WHERE ps.dailyProblem.id = :problemId AND ps.member.id = :memberId")
    int countSubmissionsByProblemAndMember(@Param("problemId") Long problemId, @Param("memberId") Long memberId);
    
    // 특정 상태의 제출 기록들 조회
    List<ProblemSubmission> findByStatus(SubmissionStatus status);
    
    // 시간 범위로 제출 기록 조회
    @Query("SELECT ps FROM ProblemSubmission ps WHERE ps.submissionTime BETWEEN :startTime AND :endTime ORDER BY ps.submissionTime DESC")
    List<ProblemSubmission> findBySubmissionTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
