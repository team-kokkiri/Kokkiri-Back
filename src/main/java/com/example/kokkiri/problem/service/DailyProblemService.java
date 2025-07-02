package com.example.kokkiri.problem.service;

import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.domain.TestCase;
import com.example.kokkiri.problem.dto.DailyProblemCreateReqDto;
import com.example.kokkiri.problem.dto.DailyProblemResDto;
import com.example.kokkiri.problem.dto.TestCaseDto;
import com.example.kokkiri.problem.repository.DailyProblemRepository;
import com.example.kokkiri.problem.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DailyProblemService {
    
    private final DailyProblemRepository dailyProblemRepository;
    private final TestCaseRepository testCaseRepository;
    
    /**
     * 오늘의 문제 조회
     */
    public Optional<DailyProblem> getTodayProblem() {
        return dailyProblemRepository.findTodayProblem();
    }
    
    /**
     * 새로운 일일 문제 등록 (DTO 버전)
     */
    @Transactional
    public DailyProblemResDto createDailyProblem(DailyProblemCreateReqDto dto) {
        // 해당 날짜에 이미 문제가 있는지 확인
        if (dailyProblemRepository.existsByProblemDate(dto.getProblemDate())) {
            throw new IllegalArgumentException("해당 날짜에 이미 문제가 등록되어 있습니다: " + dto.getProblemDate());
        }
        
        // DailyProblem 엔티티 생성
        DailyProblem dailyProblem = DailyProblem.builder()
                .problemDate(dto.getProblemDate())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .inputDescription(dto.getInputDescription())
                .outputDescription(dto.getOutputDescription())
                .sampleInput(dto.getSampleInput())
                .sampleOutput(dto.getSampleOutput())
                .timeLimit(dto.getTimeLimit())
                .memoryLimit(dto.getMemoryLimit())
                .build();
        
        DailyProblem savedProblem = dailyProblemRepository.save(dailyProblem);
        log.info("새로운 일일 문제 등록: {} - {}", savedProblem.getProblemDate(), savedProblem.getTitle());
        
        // 테스트케이스 저장
        if (dto.getTestCases() != null) {
            for (TestCaseDto testCaseDto : dto.getTestCases()) {
                TestCase testCase = TestCase.builder()
                        .dailyProblem(savedProblem)
                        .input(testCaseDto.getInput())
                        .expectedOutput(testCaseDto.getExpectedOutput())
                        .orderNum(testCaseDto.getOrderNum())
                        .isHidden(testCaseDto.getIsHidden() != null ? testCaseDto.getIsHidden() : false)
                        .build();
                testCaseRepository.save(testCase);
            }
        }
        
        return toDailyProblemResDto(savedProblem);
    }
    
    /**
     * 새로운 일일 문제 등록 (엔티티 버전 - 기존 메서드)
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
     * DailyProblem 엔티티를 ResDto로 변환
     */
    private DailyProblemResDto toDailyProblemResDto(DailyProblem problem) {
        return DailyProblemResDto.builder()
                .id(problem.getId())
                .problemDate(problem.getProblemDate())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .inputDescription(problem.getInputDescription())
                .outputDescription(problem.getOutputDescription())
                .sampleInput(problem.getSampleInput())
                .sampleOutput(problem.getSampleOutput())
                .timeLimit(problem.getTimeLimit())
                .memoryLimit(problem.getMemoryLimit())
                .build();
    }
}
