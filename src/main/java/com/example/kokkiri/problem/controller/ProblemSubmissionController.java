package com.example.kokkiri.problem.controller;

import com.example.kokkiri.common.dto.CommonResDto;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.problem.domain.ProblemSubmission;
import com.example.kokkiri.problem.domain.TestCase;
import com.example.kokkiri.problem.dto.*;
import com.example.kokkiri.problem.repository.TestCaseRepository;
import com.example.kokkiri.problem.service.DailyProblemFacadeService;
import com.example.kokkiri.problem.service.DailyRankingService;
import com.example.kokkiri.problem.service.ProblemSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final DailyRankingService rankingService;
    private final TestCaseRepository testCaseRepository;
    
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
     * 랭킹에서 클릭 시 해결 코드 조회 (내가 오늘 문제를 해결한 경우에만)
     */
    @GetMapping("/code/{submissionId}")
    public ResponseEntity<CommonResDto> getSubmissionCode(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal Member member) {
        
        try {
            // 로그인 확인
            if (member == null) {
                return new ResponseEntity<>(
                    new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null),
                    HttpStatus.UNAUTHORIZED
                );
            }
            
            Optional<ProblemSubmission> submissionOpt = submissionService.getSubmissionById(submissionId);
            
            if (submissionOpt.isEmpty()) {
                return new ResponseEntity<>(
                    new CommonResDto(HttpStatus.NOT_FOUND, "존재하지 않는 제출 기록입니다.", null),
                    HttpStatus.NOT_FOUND
                );
            }
            
            ProblemSubmission submission = submissionOpt.get();
            
            // 정답인 제출만 조회 가능 (랭킹에 올라간 코드만)
            if (!"ACCEPTED".equals(submission.getStatus().name())) {
                return new ResponseEntity<>(
                    new CommonResDto(HttpStatus.FORBIDDEN, "정답이 아닌 제출은 조회할 수 없습니다.", null),
                    HttpStatus.FORBIDDEN
                );
            }
            
            // 핵심: 내가 오늘 문제를 해결했는지 확인
            Long problemId = submission.getDailyProblem().getId();
            boolean iHaveSolved = rankingService.hasSolved(problemId, member.getId());
            
            if (!iHaveSolved) {
                return new ResponseEntity<>(
                    new CommonResDto(HttpStatus.FORBIDDEN, "오늘 문제를 먼저 해결해야 다른 사람의 코드를 볼 수 있습니다.", null),
                    HttpStatus.FORBIDDEN
                );
            }
            
            ProblemSubmissionResDto response = ProblemSubmissionResDto.from(submission);
            log.info("랭킹 코드 조회: 제출ID={}, 작성자={}, 조회자={}", 
                    submissionId, submission.getMember().getNickname(), member.getNickname());
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.OK, "해결 코드 조회 성공", response),
                HttpStatus.OK
            );
            
        } catch (Exception e) {
            log.error("해결 코드 조회 중 오류 발생: 제출ID={}", submissionId, e);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * 내 제출 상세 조회 (본인만 가능)
     */
    @GetMapping("/my/{submissionId}")
    public ResponseEntity<CommonResDto> getMySubmissionDetail(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal Member member) {
        
        if (member == null) {
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null),
                HttpStatus.UNAUTHORIZED
            );
        }
        
        try {
            Optional<ProblemSubmission> submissionOpt = submissionService.getSubmissionById(submissionId);
            
            if (submissionOpt.isEmpty()) {
                return new ResponseEntity<>(
                    new CommonResDto(HttpStatus.NOT_FOUND, "존재하지 않는 제출 기록입니다.", null),
                    HttpStatus.NOT_FOUND
                );
            }
            
            ProblemSubmission submission = submissionOpt.get();
            
            // 본인의 제출 기록만 조회 가능
            if (!submission.getMember().getId().equals(member.getId())) {
                return new ResponseEntity<>(
                    new CommonResDto(HttpStatus.FORBIDDEN, "본인의 제출 기록만 조회할 수 있습니다.", null),
                    HttpStatus.FORBIDDEN
                );
            }
            
            ProblemSubmissionResDto response = ProblemSubmissionResDto.from(submission);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.OK, "내 제출 기록 조회 성공", response),
                HttpStatus.OK
            );
            
        } catch (Exception e) {
            log.error("내 제출 기록 조회 중 오류 발생: 제출ID={}", submissionId, e);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    

    /**
     * 테스트케이스 결과 조회 (본인의 제출만 가능)
     */
    @GetMapping("/{submissionId}/testcases")
    public ResponseEntity<CommonResDto> getTestCaseResults(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal Member member) {
        
        if (member == null) {
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null),
                HttpStatus.UNAUTHORIZED
            );
        }
        
        try {
            ProblemSubmission submission = submissionService.getSubmissionById(submissionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 제출입니다."));
            
            // 본인의 제출인지 확인
            if (!submission.getMember().getId().equals(member.getId())) {
                throw new AccessDeniedException("접근 권한이 없습니다.");
            }
            
            List<TestCaseResultDto> results = submissionService.parseTestCaseResults(submission.getTestCaseResults());
            
            // 히든 테스트케이스는 결과만 표시 (입출력 숨김)
            List<TestCase> testCases = testCaseRepository.findByDailyProblemIdOrderByOrderNum(
                    submission.getDailyProblem().getId());
            
            for (int i = 0; i < results.size() && i < testCases.size(); i++) {
                if (testCases.get(i).getIsHidden()) {
                    TestCaseResultDto result = results.get(i);
                    result.setActualOutput("Hidden");
                    result.setExpectedOutput("Hidden");
                }
            }
            
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.OK, "테스트케이스 결과 조회 성공", results),
                HttpStatus.OK
            );
            
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.FORBIDDEN, e.getMessage(), null),
                HttpStatus.FORBIDDEN
            );
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.NOT_FOUND, e.getMessage(), null),
                HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            log.error("테스트케이스 결과 조회 중 오류 발생: 제출ID={}", submissionId, e);
            return new ResponseEntity<>(
                new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", null),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
