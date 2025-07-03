package com.example.kokkiri.problem.service;

import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.dto.DailyProblemResDto;
import com.example.kokkiri.problem.dto.ProblemListResDto;
import com.example.kokkiri.problem.repository.DailyProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * 해당 날짜에 문제가 존재하는지 확인
     */
    public boolean existsProblemOnDate(LocalDate date) {
        return dailyProblemRepository.existsByProblemDate(date);
    }
    
    /**
     * 전체 문제 목록 조회 (페이징)
     */
    public ProblemListResDto getProblemList(int page, int size, String sortBy, String sortDir, Long memberId) {
        // 정렬 방향 설정
        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        
        // 페이지 요청 객체 생성
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // 활성화된 문제들 조회
        Page<DailyProblem> problemPage = dailyProblemRepository.findByIsActive("Y", pageable);
        
        // DTO 변환 및 추가 정보 설정
        List<DailyProblemResDto> problemDtos = problemPage.getContent().stream()
                .map(problem -> {
                    // 해결자 수 조회
                    Long solverCount = dailyProblemRepository.countSolversByProblemId(problem.getId());
                    
                    // 사용자가 해결했는지 확인 (로그인한 경우에만)
                    Boolean hasSolved = null;
                    if (memberId != null) {
                        hasSolved = dailyProblemRepository.hasUserSolvedProblem(problem.getId(), memberId);
                    }
                    
                    return DailyProblemResDto.fromWithSolverInfo(problem, solverCount, hasSolved);
                })
                .collect(Collectors.toList());
        
        // 페이징 정보와 함께 응답 DTO 생성
        return ProblemListResDto.of(
                problemDtos,
                problemPage.getNumber(),
                problemPage.getTotalPages(),
                problemPage.getTotalElements(),
                problemPage.getSize(),
                problemPage.isFirst(),
                problemPage.isLast(),
                problemPage.hasNext(),
                problemPage.hasPrevious()
        );
    }
    
    /**
     * 특정 날짜의 문제 조회
     */
    public Optional<DailyProblem> getProblemByDate(LocalDate date) {
        return dailyProblemRepository.findByProblemDateAndIsActive(date, "Y");
    }
    
    /**
     * 문제 수정
     */
    @Transactional
    public DailyProblem updateDailyProblem(Long problemId, DailyProblem updatedProblem) {
        DailyProblem existingProblem = dailyProblemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다: " + problemId));
        
        // 업데이트할 필드들 설정
        existingProblem.setTitle(updatedProblem.getTitle());
        existingProblem.setDescription(updatedProblem.getDescription());
        existingProblem.setInputDescription(updatedProblem.getInputDescription());
        existingProblem.setOutputDescription(updatedProblem.getOutputDescription());
        existingProblem.setSampleInput(updatedProblem.getSampleInput());
        existingProblem.setSampleOutput(updatedProblem.getSampleOutput());
        existingProblem.setTimeLimit(updatedProblem.getTimeLimit());
        existingProblem.setMemoryLimit(updatedProblem.getMemoryLimit());
        
        log.info("문제 수정: {} - {}", existingProblem.getProblemDate(), existingProblem.getTitle());
        return dailyProblemRepository.save(existingProblem);
    }
    
    /**
     * 문제 삭제 (비활성화)
     */
    @Transactional
    public void deleteDailyProblem(Long problemId) {
        DailyProblem problem = dailyProblemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다: " + problemId));
        
        problem.setIsActive("N");
        log.info("문제 삭제(비활성화): {} - {}", problem.getProblemDate(), problem.getTitle());
        dailyProblemRepository.save(problem);
    }
}
