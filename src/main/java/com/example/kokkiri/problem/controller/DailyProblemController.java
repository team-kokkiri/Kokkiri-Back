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

import java.time.LocalDate;
import java.util.Optional;

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
     * 전체 문제 목록 조회 (페이징)
     */
    @GetMapping("/list")
    public ResponseEntity<CommonResDto> getProblemList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "problemDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @AuthenticationPrincipal Member member) {
        try {
            Long memberId = member != null ? member.getId() : null;
            ProblemListResDto response = dailyProblemService.getProblemList(page, size, sortBy, sortDir, memberId);
            
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 목록 조회 성공", response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("문제 목록 조회 중 오류 발생", e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 특정 날짜의 문제 조회
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<CommonResDto> getProblemByDate(@PathVariable String date) {
        try {
            LocalDate problemDate = LocalDate.parse(date);
            Optional<DailyProblem> problem = dailyProblemService.getProblemByDate(problemDate);
            
            if (problem.isPresent()) {
                DailyProblemResDto response = DailyProblemResDto.from(problem.get());
                return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 조회 성공", response), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, "해당 날짜의 문제를 찾을 수 없습니다.", null), HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("문제 조회 중 오류 발생", e);
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
    
    /**
     * 문제 수정 (관리자 전용)
     */
    @PutMapping("/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> updateDailyProblem(
            @PathVariable Long problemId,
            @RequestBody DailyProblemCreateReqDto requestDto) {
        try {
            // 업데이트할 엔티티 생성
            DailyProblem updatedProblem = DailyProblem.builder()
                    .title(requestDto.getTitle())
                    .description(requestDto.getDescription())
                    .inputDescription(requestDto.getInputDescription())
                    .outputDescription(requestDto.getOutputDescription())
                    .sampleInput(requestDto.getSampleInput())
                    .sampleOutput(requestDto.getSampleOutput())
                    .timeLimit(requestDto.getTimeLimit())
                    .memoryLimit(requestDto.getMemoryLimit())
                    .build();
            
            DailyProblem savedProblem = dailyProblemService.updateDailyProblem(problemId, updatedProblem);
            DailyProblemResDto response = DailyProblemResDto.from(savedProblem);
            
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 수정 성공", response), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("문제 수정 중 오류 발생", e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 문제 삭제 (관리자 전용)
     */
    @DeleteMapping("/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteDailyProblem(@PathVariable Long problemId) {
        try {
            dailyProblemService.deleteDailyProblem(problemId);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 삭제 성공", null), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("문제 삭제 중 오류 발생", e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
