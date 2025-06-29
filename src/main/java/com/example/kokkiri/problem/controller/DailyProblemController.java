package com.example.kokkiri.problem.controller;

import com.example.kokkiri.common.dto.CommonResDto;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.dto.*;
import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import com.example.kokkiri.problem.service.DailyProblemService;
import com.example.kokkiri.problem.service.DailyRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Slf4j
public class DailyProblemController {
    
    private final DailyProblemService dailyProblemService;
    private final DailyProblemFacadeService facadeService;
    private final DailyRankingService rankingService;
    
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
     * 특정 날짜의 문제 조회
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<CommonResDto> getProblemByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal Member member) {
        try {
            Long memberId = member != null ? member.getId() : null;
            DailyProblemFacadeService.ProblemDetailInfo detailInfo = facadeService.getProblemDetailInfo(date, memberId);
            
            if (detailInfo.problem == null) {
                return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, "해당 날짜에 문제가 없습니다.", null), HttpStatus.NOT_FOUND);
            }
            
            ProblemDetailInfoResDto response = ProblemDetailInfoResDto.from(detailInfo);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 조회 성공", response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("날짜별 문제 조회 중 오류 발생: {}", date, e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 문제 ID로 상세 조회
     */
    @GetMapping("/{problemId}")
    public ResponseEntity<CommonResDto> getProblemById(
            @PathVariable Long problemId,
            @AuthenticationPrincipal Member member) {
        try {
            Optional<DailyProblem> problemOpt = dailyProblemService.getProblemById(problemId);
            
            if (problemOpt.isEmpty()) {
                return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, "존재하지 않는 문제입니다.", null), HttpStatus.NOT_FOUND);
            }
            
            DailyProblem problem = problemOpt.get();
            Long memberId = member != null ? member.getId() : null;
            
            // 해결 여부와 해결자 수 조회
            boolean hasSolved = memberId != null && rankingService.hasSolved(problemId, memberId);
            long solverCount = rankingService.getSolverCount(problemId);
            
            DailyProblemResDto response = DailyProblemResDto.fromWithSolverInfo(problem, solverCount, hasSolved);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 조회 성공", response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("문제 상세 조회 중 오류 발생: {}", problemId, e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 최근 문제 목록 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<CommonResDto> getRecentProblems(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal Member member) {
        try {
            Long memberId = member != null ? member.getId() : null;
            List<DailyProblemFacadeService.ProblemSummary> summaries = facadeService.getRecentProblemsWithStatus(memberId, days);
            
            List<ProblemSummaryResDto> response = summaries.stream()
                    .map(ProblemSummaryResDto::from)
                    .toList();
            
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "최근 문제 조회 성공", response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("최근 문제 조회 중 오류 발생", e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 활성화된 모든 문제 목록 조회
     */
    @GetMapping("/list")
    public ResponseEntity<CommonResDto> getAllActiveProblems(@AuthenticationPrincipal Member member) {
        try {
            List<DailyProblem> problems = dailyProblemService.getActiveProblems();
            Long memberId = member != null ? member.getId() : null;
            
            List<DailyProblemListResDto> response = problems.stream()
                    .map(problem -> {
                        boolean hasSolved = memberId != null && rankingService.hasSolved(problem.getId(), memberId);
                        long solverCount = rankingService.getSolverCount(problem.getId());
                        return DailyProblemListResDto.fromWithStats(problem, solverCount, hasSolved, 0);
                    })
                    .toList();
            
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 목록 조회 성공", response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("문제 목록 조회 중 오류 발생", e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 날짜 범위로 문제 조회
     */
    @GetMapping("/range")
    public ResponseEntity<CommonResDto> getProblemsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal Member member) {
        try {
            if (startDate.isAfter(endDate)) {
                return new ResponseEntity<>(new CommonResDto(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 이전이어야 합니다.", null), HttpStatus.BAD_REQUEST);
            }
            
            List<DailyProblem> problems = dailyProblemService.getProblemsByDateRange(startDate, endDate);
            Long memberId = member != null ? member.getId() : null;
            
            List<DailyProblemListResDto> response = problems.stream()
                    .map(problem -> {
                        boolean hasSolved = memberId != null && rankingService.hasSolved(problem.getId(), memberId);
                        long solverCount = rankingService.getSolverCount(problem.getId());
                        return DailyProblemListResDto.fromWithStats(problem, solverCount, hasSolved, 0);
                    })
                    .toList();
            
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "날짜 범위 문제 조회 성공", response), HttpStatus.OK);
        } catch (Exception e) {
            log.error("날짜 범위 문제 조회 중 오류 발생: {} ~ {}", startDate, endDate, e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // === 관리자 전용 API ===
    
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
     * 일일 문제 수정 (관리자 전용)
     */
    @PutMapping("/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> updateDailyProblem(
            @PathVariable Long problemId,
            @RequestBody DailyProblemUpdateReqDto requestDto) {
        try {
            // 수정할 데이터로 엔티티 생성
            DailyProblem updateData = DailyProblem.builder()
                    .title(requestDto.getTitle())
                    .description(requestDto.getDescription())
                    .inputDescription(requestDto.getInputDescription())
                    .outputDescription(requestDto.getOutputDescription())
                    .sampleInput(requestDto.getSampleInput())
                    .sampleOutput(requestDto.getSampleOutput())
                    .timeLimit(requestDto.getTimeLimit())
                    .memoryLimit(requestDto.getMemoryLimit())
                    .build();
            
            DailyProblem updatedProblem = dailyProblemService.updateDailyProblem(problemId, updateData);
            DailyProblemResDto response = DailyProblemResDto.from(updatedProblem);
            
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 수정 성공", response), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("문제 수정 중 오류 발생: {}", problemId, e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 일일 문제 비활성화 (관리자 전용)
     */
    @PatchMapping("/{problemId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deactivateProblem(@PathVariable Long problemId) {
        try {
            dailyProblemService.deactivateProblem(problemId);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 비활성화 성공", null), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("문제 비활성화 중 오류 발생: {}", problemId, e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 일일 문제 활성화 (관리자 전용)
     */
    @PatchMapping("/{problemId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> activateProblem(@PathVariable Long problemId) {
        try {
            dailyProblemService.activateProblem(problemId);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 활성화 성공", null), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("문제 활성화 중 오류 발생: {}", problemId, e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 일일 문제 삭제 (관리자 전용)
     */
    @DeleteMapping("/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteProblem(@PathVariable Long problemId) {
        try {
            dailyProblemService.deleteProblem(problemId);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 삭제 성공", null), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, e.getMessage(), null), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("문제 삭제 중 오류 발생: {}", problemId, e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 특정 날짜에 문제 존재 여부 확인 (관리자 전용)
     */
    @GetMapping("/exists/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> checkProblemExists(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            boolean exists = dailyProblemService.existsProblemOnDate(date);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "문제 존재 여부 확인 성공", exists), HttpStatus.OK);
        } catch (Exception e) {
            log.error("문제 존재 여부 확인 중 오류 발생: {}", date, e);
            return new ResponseEntity<>(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
