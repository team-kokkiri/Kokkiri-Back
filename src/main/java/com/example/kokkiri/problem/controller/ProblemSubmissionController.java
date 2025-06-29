package com.example.kokkiri.problem.controller;

import com.example.kokkiri.common.dto.CommonResDto;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.problem.domain.ProblemSubmission;
import com.example.kokkiri.problem.dto.*;
import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import com.example.kokkiri.problem.service.ProblemSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Slf4j
public class ProblemSubmissionController {
    
    private final ProblemSubmissionService submissionService;
    private final DailyProblemFacadeService facadeService;
    
    /**
     * 코드 제출 및 채점
     */
    @PostMapping
    public Mono<ResponseEntity<CommonResDto>> submitCode(
            @RequestBody ProblemSubmissionReqDto requestDto,
            @AuthenticationPrincipal Member member) {
        
        if (member == null) {
            return Mono.just(new ResponseEntity<>(
                new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null), 
                HttpStatus.UNAUTHORIZED
            ));
        }
        
        try {
            // 입력값 검증
            if (requestDto.getProblemId() == null) {
                return Mono.just(new ResponseEntity<>(
                    new CommonResDto(HttpStatus.BAD_REQUEST, "문제 ID는 필수입니다.", null),
                    HttpStatus.BAD_REQUEST
                ));
            }
            
            if (requestDto.getSourceCode() == null || requestDto.getSourceCode().trim().isEmpty()) {
                return Mono.just(new ResponseEntity<>(
                    new CommonResDto(HttpStatus.BAD_REQUEST, "소스코드는 필수입니다.", null),
                    HttpStatus.BAD_REQUEST
                ));
            }
            
            if (requestDto.getLanguage() == null || requestDto.getLanguage().trim().isEmpty()) {
                requestDto.setLanguage("JAVA"); // 기본값 설정
            }
            
            log.info("코드 제출 요청: 문제ID={}, 회원ID={}, 언어={}", 
                    requestDto.getProblemId(), member.getId(), requestDto.getLanguage());
            
            // 통합 서비스를 통한 제출 처리 (제출 → 채점 → 랭킹 업데이트)
            return facadeService.submitAndProcess(
                    requestDto.getProblemId(),
                    member.getId(),
                    requestDto.getSourceCode(),
                    requestDto.getLanguage()
            ).map(result -> {
                if (result.success) {
                    SubmissionResultResDto response = SubmissionResultResDto.from(result);
                    return new ResponseEntity<>(
                        new CommonResDto(HttpStatus.OK, result.message, response),
                        HttpStatus.OK
                    );
                } else {
                    return new ResponseEntity<>(
                        new CommonResDto(HttpStatus.BAD_REQUEST, result.message, null),
                        HttpStatus.BAD_REQUEST
                    );
                }
            }).onErrorResume(error -> {
                log.error("코드 제출 중 오류 발생: 문제ID={}, 회원ID={}", 
                         requestDto.getProblemId(), member.getId(), error);
                return Mono.just(new ResponseEntity<>(
                    new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                    HttpStatus.INTERNAL_SERVER_ERROR
                ));
            });
            
        } catch (Exception e) {
            log.error("코드 제출 요청 처리 중 오류 발생", e);
            return Mono.just(new ResponseEntity<>(
                new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                HttpStatus.INTERNAL_SERVER_ERROR
            ));
        }
    }
    
    /**
     * 제출 상세 조회
     */
    @GetMapping("/{submissionId}")
    public ResponseEntity<CommonResDto> getSubmissionDetail(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal Member member) {
        
        try {
            Optional<ProblemSubmission> submissionOpt = submissionService.getSubmissionById(submissionId);
            
            if (submissionOpt.isEmpty()) {
                return new ResponseEntity<>(
                    new CommonResDto(HttpStatus.NOT_FOUND, "존재하지 않는 제출 기록입니다.", null),
                    HttpStatus.NOT_FOUND
                );
            }
            
            ProblemSubmission submission = submissionOpt.get();
            
            // 본인의 제출 기록만 조회 가능 (관리자는 모든 기록 조회 가능)
            if (member == null || 
                (!submission.getMember().getId().equals(member.getId()) && 
                 !member.getRole().name().equals("ADMIN"))) {
                return new ResponseEntity<>(
                    new CommonResDto(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", null),
                    HttpStatus.FORBIDDEN
                );
            }
            
            ProblemSubmissionResDto response = ProblemSubmissionResDto.from(submission);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.OK, "제출 기록 조회 성공", response),
                HttpStatus.OK
            );
            
        } catch (Exception e) {
            log.error("제출 기록 조회 중 오류 발생: 제출ID={}", submissionId, e);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * 내 제출 기록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<CommonResDto> getMySubmissions(@AuthenticationPrincipal Member member) {
        
        if (member == null) {
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null),
                HttpStatus.UNAUTHORIZED
            );
        }
        
        try {
            List<ProblemSubmission> submissions = submissionService.getMemberAllSubmissions(member.getId());
            
            List<ProblemSubmissionResDto> response = submissions.stream()
                    .map(ProblemSubmissionResDto::fromWithoutSourceCode) // 목록에서는 소스코드 제외
                    .toList();
            
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.OK, "내 제출 기록 조회 성공", response),
                HttpStatus.OK
            );
            
        } catch (Exception e) {
            log.error("내 제출 기록 조회 중 오류 발생: 회원ID={}", member.getId(), e);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * 특정 문제에 대한 내 제출 기록 조회
     */
    @GetMapping("/my/problem/{problemId}")
    public ResponseEntity<CommonResDto> getMySubmissionsForProblem(
            @PathVariable Long problemId,
            @AuthenticationPrincipal Member member) {
        
        if (member == null) {
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null),
                HttpStatus.UNAUTHORIZED
            );
        }
        
        try {
            List<ProblemSubmission> submissions = submissionService.getMemberSubmissions(problemId, member.getId());
            
            List<ProblemSubmissionResDto> response = submissions.stream()
                    .map(ProblemSubmissionResDto::from) // 개별 문제는 소스코드 포함
                    .toList();
            
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.OK, "문제별 제출 기록 조회 성공", response),
                HttpStatus.OK
            );
            
        } catch (Exception e) {
            log.error("문제별 제출 기록 조회 중 오류 발생: 문제ID={}, 회원ID={}", problemId, member.getId(), e);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * 특정 문제의 모든 제출 기록 조회 (관리자 전용)
     */
    @GetMapping("/problem/{problemId}")
    public ResponseEntity<CommonResDto> getProblemSubmissions(
            @PathVariable Long problemId,
            @AuthenticationPrincipal Member member) {
        
        if (member == null || !member.getRole().name().equals("ADMIN")) {
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.", null),
                HttpStatus.FORBIDDEN
            );
        }
        
        try {
            List<ProblemSubmission> submissions = submissionService.getProblemSubmissions(problemId);
            
            List<ProblemSubmissionResDto> response = submissions.stream()
                    .map(ProblemSubmissionResDto::fromWithoutSourceCode) // 관리자도 목록에서는 소스코드 제외
                    .toList();
            
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.OK, "문제 제출 기록 조회 성공", response),
                HttpStatus.OK
            );
            
        } catch (Exception e) {
            log.error("문제 제출 기록 조회 중 오류 발생: 문제ID={}", problemId, e);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * 특정 문제의 해결 여부 확인
     */
    @GetMapping("/solved/{problemId}")
    public ResponseEntity<CommonResDto> checkSolved(
            @PathVariable Long problemId,
            @AuthenticationPrincipal Member member) {
        
        if (member == null) {
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null),
                HttpStatus.UNAUTHORIZED
            );
        }
        
        try {
            boolean hasSolved = submissionService.hasSolved(problemId, member.getId());
            int submissionCount = submissionService.getSubmissionCount(problemId, member.getId());
            
            SolvedStatusResDto response = SolvedStatusResDto.builder()
                    .problemId(problemId)
                    .memberId(member.getId())
                    .hasSolved(hasSolved)
                    .submissionCount(submissionCount)
                    .build();
            
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.OK, "해결 여부 조회 성공", response),
                HttpStatus.OK
            );
            
        } catch (Exception e) {
            log.error("해결 여부 조회 중 오류 발생: 문제ID={}, 회원ID={}", problemId, member.getId(), e);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    // 내부 클래스: 해결 상태 응답 DTO
    @lombok.Builder
    @lombok.Getter
    public static class SolvedStatusResDto {
        private Long problemId;
        private Long memberId;
        private boolean hasSolved;
        private int submissionCount;
    }
}
