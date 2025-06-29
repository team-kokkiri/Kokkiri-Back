package com.example.kokkiri.problem.service;

import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.repository.DailyProblemRepository;
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
}
