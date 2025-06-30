package com.example.kokkiri.problem.controller;

import com.example.kokkiri.common.dto.CommonResDto;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.dto.*;
import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import com.example.kokkiri.problem.service.DailyProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Slf4j
public class DailyProblemController {
    
    private final DailyProblemService dailyProblemService;
    private final DailyProblemFacadeService facadeService;
    
    /**
     * 오늘의 문제 조회 (랭킹 정보 포함)
     */
    @GetMapping("/today")
    public ResponseEntity<CommonResDto> getTodayProblem(@AuthenticationPrincipal Member member) {
        try {
            Long memberId = member != null ? member.getId() : null;
            DailyProblemFacadeService.TodayProblemInfo todayInfo = facadeService.getTodayProblemInfo(memberId);
            TodayProblemInfoResDto response = TodayProblemInfoResDto.from(todayInfo);
            
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "오늘의 문제 조회 성공", response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("오늘의 문제 조회 중 오류 발생", e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 새로운 일일 문제 생성 (관리자 전용)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> createDailyProblem(@RequestBody DailyProblemCreateReqDto requestDto) {
        try {
            // 해당 날짜에 이미 문제가 있는지 확인
            if (dailyProblemService.existsProblemOnDate(requestDto.getProblemDate())) {
                return new ResponseEntity<>(
                    new CommonResDto(HttpStatus.CONFLICT, "해당 날짜에 이미 문제가 등록되어 있습니다.", null), 
                    HttpStatus.CONFLICT
                );
            }
            
            // 엔티티 생성
            DailyProblem dailyProblem = DailyProblem.builder()
                    .problemDate(requestDto.getProblemDate())
                    .title(requestDto.getTitle())
                    .description(requestDto.getDescription())
                    .inputDescription(requestDto.getInputDescription())
                    .outputDescription(requestDto.getOutputDescription())
                    .sampleInput(requestDto.getSampleInput())
                    .sampleOutput(requestDto.getSampleOutput())
                    .timeLimit(requestDto.getTimeLimit())
                    .memoryLimit(requestDto.getMemoryLimit())
                    .build();
            
            DailyProblem savedProblem = dailyProblemService.createDailyProblem(dailyProblem);
            DailyProblemResDto response = DailyProblemResDto.from(savedProblem);
            
            return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "문제 생성 성공", response), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new CommonResDto(HttpStatus.CONFLICT, e.getMessage(), null), HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("문제 생성 중 오류 발생", e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
