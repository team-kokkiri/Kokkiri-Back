package com.example.kokkiri.problem.service;

import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.repository.DailyProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DailyProblemService {
    
    private final DailyProblemRepository dailyProblemRepository;
    
    /**
     * 오늘의 문제 조회
     */
    public Optional<DailyProblem> getTodayProblem() {
        return dailyProblemRepository.findTodayProblem();
    }
    
    /**
     * 특정 날짜의 문제 조회
     */
    public Optional<DailyProblem> getProblemByDate(LocalDate date) {
        return dailyProblemRepository.findByProblemDate(date);
    }
    
    /**
     * 문제 ID로 조회
     */
    public Optional<DailyProblem> getProblemById(Long problemId) {
        return dailyProblemRepository.findById(problemId);
    }
    
    /**
     * 최근 활성화된 문제들 조회 (최신순)
     */
    public List<DailyProblem> getActiveProblems() {
        return dailyProblemRepository.findActiveProblemsOrderByDateDesc();
    }
    
    /**
     * 최근 N일간의 문제 조회
     */
    public List<DailyProblem> getRecentProblems(int days) {
        LocalDate fromDate = LocalDate.now().minusDays(days - 1);
        return dailyProblemRepository.findRecentProblems(fromDate);
    }
    
    /**
     * 날짜 범위로 문제 조회
     */
    public List<DailyProblem> getProblemsByDateRange(LocalDate startDate, LocalDate endDate) {
        return dailyProblemRepository.findByProblemDateBetween(startDate, endDate);
    }
    
    /**
     * 새로운 일일 문제 등록
     */
    @Transactional
    public DailyProblem createDailyProblem(DailyProblem dailyProblem) {
        // 해당 날짜에 이미 문제가 있는지 확인
        if (dailyProblemRepository.existsByProblemDate(dailyProblem.getProblemDate())) {
            throw new IllegalArgumentException("해당 날짜에 이미 문제가 등록되어 있습니다: " + dailyProblem.getProblemDate());
        }
        
        log.info("새로운 일일 문제 등록: {} - {}", dailyProblem.getProblemDate(), dailyProblem.getTitle());
        return dailyProblemRepository.save(dailyProblem);
    }
    
    /**
     * 일일 문제 수정
     */
    @Transactional
    public DailyProblem updateDailyProblem(Long problemId, DailyProblem updateData) {
        DailyProblem existingProblem = dailyProblemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다: " + problemId));
        
        // 수정 가능한 필드들 업데이트
        existingProblem.setTitle(updateData.getTitle());
        existingProblem.setDescription(updateData.getDescription());
        existingProblem.setInputDescription(updateData.getInputDescription());
        existingProblem.setOutputDescription(updateData.getOutputDescription());
        existingProblem.setSampleInput(updateData.getSampleInput());
        existingProblem.setSampleOutput(updateData.getSampleOutput());
        existingProblem.setTimeLimit(updateData.getTimeLimit());
        existingProblem.setMemoryLimit(updateData.getMemoryLimit());
        
        log.info("일일 문제 수정: {} - {}", existingProblem.getProblemDate(), existingProblem.getTitle());
        return dailyProblemRepository.save(existingProblem);
    }
    
    /**
     * 일일 문제 비활성화
     */
    @Transactional
    public void deactivateProblem(Long problemId) {
        DailyProblem problem = dailyProblemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다: " + problemId));
        
        problem.setIsActive("N");
        dailyProblemRepository.save(problem);
        log.info("일일 문제 비활성화: {} - {}", problem.getProblemDate(), problem.getTitle());
    }
    
    /**
     * 일일 문제 활성화
     */
    @Transactional
    public void activateProblem(Long problemId) {
        DailyProblem problem = dailyProblemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다: " + problemId));
        
        problem.setIsActive("Y");
        dailyProblemRepository.save(problem);
        log.info("일일 문제 활성화: {} - {}", problem.getProblemDate(), problem.getTitle());
    }
    
    /**
     * 해당 날짜에 문제가 존재하는지 확인
     */
    public boolean existsProblemOnDate(LocalDate date) {
        return dailyProblemRepository.existsByProblemDate(date);
    }
    
    /**
     * 문제 삭제 (관리자용)
     */
    @Transactional
    public void deleteProblem(Long problemId) {
        DailyProblem problem = dailyProblemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다: " + problemId));
        
        log.warn("일일 문제 삭제: {} - {}", problem.getProblemDate(), problem.getTitle());
        dailyProblemRepository.delete(problem);
    }
}
