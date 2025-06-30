package com.example.kokkiri.problem.service;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.domain.ProblemSubmission;
import com.example.kokkiri.problem.domain.SubmissionStatus;
import com.example.kokkiri.problem.repository.DailyProblemRepository;
import com.example.kokkiri.problem.repository.ProblemSubmissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ProblemSubmissionService {
    
    private final ProblemSubmissionRepository submissionRepository;
    private final DailyProblemRepository dailyProblemRepository;
    private final MemberRepository memberRepository;
    private final WebClient webClient;
    
    // DailyRankingService는 @Lazy로 주입하여 순환 참조 방지
    @Lazy
    private final DailyRankingService dailyRankingService;
    
    public ProblemSubmissionService(ProblemSubmissionRepository submissionRepository,
                                  DailyProblemRepository dailyProblemRepository,
                                  MemberRepository memberRepository,
                                  @Lazy DailyRankingService dailyRankingService,
                                  WebClient.Builder webClientBuilder,
                                  @Value("${judge0.api-key}") String apiKey) {
        this.submissionRepository = submissionRepository;
        this.dailyProblemRepository = dailyProblemRepository;
        this.memberRepository = memberRepository;
        this.dailyRankingService = dailyRankingService;
        this.webClient = webClientBuilder
                .baseUrl("https://judge0-ce.p.rapidapi.com")
                .defaultHeader("x-rapidapi-key", apiKey)
                .defaultHeader("x-rapidapi-host", "judge0-ce.p.rapidapi.com")
                .build();
    }
    
    /**
     * 코드 제출 및 채점 처리
     */
    @Transactional
    public Mono<ProblemSubmission> submitCode(Long problemId, Long memberId, String sourceCode, String language) {
        // 문제와 회원 조회
        DailyProblem problem = dailyProblemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다: " + problemId));
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다: " + memberId));
        
        // 제출 기록 생성
        ProblemSubmission submission = ProblemSubmission.builder()
                .dailyProblem(problem)
                .member(member)
                .sourceCode(sourceCode)
                .language(language)
                .status(SubmissionStatus.PENDING)
                .submissionTime(LocalDateTime.now())
                .build();
        
        // DB에 저장
        ProblemSubmission savedSubmission = submissionRepository.save(submission);
        log.info("코드 제출 기록 생성: 문제ID={}, 회원ID={}, 제출ID={}", problemId, memberId, savedSubmission.getId());
        
        // 연관 엔티티를 포함하여 다시 조회
        ProblemSubmission fetchedSubmission = submissionRepository.findByIdWithFetch(savedSubmission.getId())
                .orElse(savedSubmission);
        
        // Judge0를 통한 채점 수행 (비동기)
        return executeJudging(fetchedSubmission, problem)
                .doOnError(error -> log.error("채점 처리 중 오류 발생: 제출ID={}", fetchedSubmission.getId(), error));
    }
    
    /**
     * Judge0를 통한 코드 실행 및 채점
     */
    private Mono<ProblemSubmission> executeJudging(ProblemSubmission submission, DailyProblem problem) {
        String encodedSourceCode = Base64.getEncoder().encodeToString(submission.getSourceCode().getBytes());
        String encodedInput = "";
        
        if (problem.getSampleInput() != null) {
            encodedInput = Base64.getEncoder().encodeToString(problem.getSampleInput().getBytes());
        }
        
        Map<String, String> requestPayload = new HashMap<>();
        requestPayload.put("language_id", getLanguageId(submission.getLanguage()));
        requestPayload.put("source_code", encodedSourceCode);
        requestPayload.put("stdin", encodedInput);
        
        return webClient.post()
                .uri("/submissions?base64_encoded=true&wait=false")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    String token = (String) response.get("token");
                    return pollJudgeResult(token, submission, problem);
                });
    }
    
    /**
     * Judge0 결과 폴링
     */
    private Mono<ProblemSubmission> pollJudgeResult(String token, ProblemSubmission submission, DailyProblem problem) {
        return Mono.defer(() -> getJudgeResult(token))
                .expand(result -> {
                    Map status = (Map) result.get("status");
                    Integer statusId = status != null ? (Integer) status.get("id") : null;
                    // 1: In Queue, 2: Processing
                    if (statusId != null && (statusId == 1 || statusId == 2)) {
                        return Mono.delay(java.time.Duration.ofMillis(800)).then(getJudgeResult(token));
                    } else {
                        return Mono.empty();
                    }
                })
                .last()
                .map(result -> updateSubmissionWithResult(submission, result, problem));
    }
    
    /**
     * Judge0 결과 조회
     */
    private Mono<Map> getJudgeResult(String token) {
        return webClient.get()
                .uri("/submissions/" + token + "?base64_encoded=true")
                .retrieve()
                .bodyToMono(Map.class);
    }
    
    /**
     * 제출 기록을 Judge0 결과로 업데이트
     */
    @Transactional
    public ProblemSubmission updateSubmissionWithResult(ProblemSubmission submission, Map judgeResult, DailyProblem problem) {
        try {
            // DB에서 연관 엔티티를 포함하여 다시 조회
            ProblemSubmission managedSubmission = submissionRepository.findByIdWithFetch(submission.getId())
                    .orElseThrow(() -> new IllegalStateException("제출 기록을 찾을 수 없습니다: " + submission.getId()));
            
            // 실행 결과 파싱
            String stdout = (String) judgeResult.get("stdout");
            String stderr = (String) judgeResult.get("stderr");
            String compileOutput = (String) judgeResult.get("compile_output");
            Map status = (Map) judgeResult.get("status");
            
            // 실행 시간 및 메모리 사용량
            Object time = judgeResult.get("time");
            Object memory = judgeResult.get("memory");
            
            if (time != null) {
                managedSubmission.setExecutionTime((int) (Double.parseDouble(time.toString()) * 1000)); // ms로 변환
            }
            if (memory != null) {
                managedSubmission.setMemoryUsage(Integer.parseInt(memory.toString()));
            }
            
            // 상태 결정
            SubmissionStatus submissionStatus = determineStatus(status, stdout, problem);
            managedSubmission.setStatus(submissionStatus);
            
            // 결과 메시지 구성
            StringBuilder resultMessage = new StringBuilder();
            if (stdout != null) {
                resultMessage.append("Output: ").append(new String(Base64.getDecoder().decode(stdout))).append("\n");
            }
            if (stderr != null) {
                resultMessage.append("Error: ").append(new String(Base64.getDecoder().decode(stderr))).append("\n");
            }
            if (compileOutput != null) {
                resultMessage.append("Compile: ").append(new String(Base64.getDecoder().decode(compileOutput)));
            }
            
            managedSubmission.setJudgeResult(resultMessage.toString());
            managedSubmission.setJudgeTime(LocalDateTime.now());
            
            // 에러 메시지 설정
            if (submissionStatus != SubmissionStatus.ACCEPTED) {
                managedSubmission.setErrorMessage(getErrorMessage(submissionStatus, resultMessage.toString()));
            }
            
            log.info("채점 완료: 제출ID={}, 상태={}", managedSubmission.getId(), submissionStatus);
            ProblemSubmission savedSubmission = submissionRepository.save(managedSubmission);
            
            // 정답인 경우 랭킹 업데이트 (트랜잭션 내에서 처리)
            if (submissionStatus == SubmissionStatus.ACCEPTED) {
                try {
                    dailyRankingService.updateRanking(savedSubmission);
                } catch (Exception e) {
                    log.error("랭킹 업데이트 실패: 제출ID={}", savedSubmission.getId(), e);
                }
            }
            
            // 연관 엔티티를 포함하여 다시 조회하여 반환
            return submissionRepository.findByIdWithFetch(savedSubmission.getId())
                    .orElse(savedSubmission);
            
        } catch (Exception e) {
            log.error("채점 결과 처리 중 오류 발생: 제출ID={}", submission.getId(), e);
            
            // 실패 시에도 관리되는 엔티티로 업데이트
            ProblemSubmission managedSubmission = submissionRepository.findByIdWithFetch(submission.getId())
                    .orElse(submission);
            
            managedSubmission.setStatus(SubmissionStatus.RUNTIME_ERROR);
            managedSubmission.setErrorMessage("채점 처리 중 오류가 발생했습니다.");
            managedSubmission.setJudgeTime(LocalDateTime.now());
            ProblemSubmission saved = submissionRepository.save(managedSubmission);
            
            // 연관 엔티티를 포함하여 다시 조회하여 반환
            return submissionRepository.findByIdWithFetch(saved.getId())
                    .orElse(saved);
        }
    }
    
    /**
     * Judge0 상태와 출력을 기반으로 제출 상태 결정
     */
    private SubmissionStatus determineStatus(Map status, String stdout, DailyProblem problem) {
        if (status == null) return SubmissionStatus.RUNTIME_ERROR;
        
        Integer statusId = (Integer) status.get("id");
        
        switch (statusId) {
            case 3: // Accepted
                // 출력 결과와 예상 출력 비교
                if (stdout != null && problem.getSampleOutput() != null) {
                    String actualOutput = new String(Base64.getDecoder().decode(stdout)).trim();
                    String expectedOutput = problem.getSampleOutput().trim();
                    return actualOutput.equals(expectedOutput) ? SubmissionStatus.ACCEPTED : SubmissionStatus.WRONG_ANSWER;
                }
                return SubmissionStatus.ACCEPTED;
            case 4: // Wrong Answer
                return SubmissionStatus.WRONG_ANSWER;
            case 5: // Time Limit Exceeded
                return SubmissionStatus.TIME_LIMIT_EXCEEDED;
            case 6: // Compilation Error
                return SubmissionStatus.COMPILE_ERROR;
            case 7: // Runtime Error (SIGSEGV)
            case 8: // Runtime Error (SIGXFSZ)
            case 9: // Runtime Error (SIGFPE)
            case 10: // Runtime Error (SIGABRT)
            case 11: // Runtime Error (NZEC)
            case 12: // Runtime Error (Other)
                return SubmissionStatus.RUNTIME_ERROR;
            case 13: // Internal Error
                return SubmissionStatus.RUNTIME_ERROR;
            case 14: // Exec Format Error
                return SubmissionStatus.COMPILE_ERROR;
            default:
                return SubmissionStatus.RUNTIME_ERROR;
        }
    }
    
    /**
     * 언어에 따른 Judge0 언어 ID 반환
     */
    private String getLanguageId(String language) {
        switch (language.toUpperCase()) {
            case "JAVA": return "62";
            case "PYTHON": return "71";
            case "C": return "50";
            case "CPP": case "C++": return "54";
            case "JAVASCRIPT": return "63";
            default: return "62"; // 기본값: Java
        }
    }
    
    /**
     * 상태에 따른 에러 메시지 생성
     */
    private String getErrorMessage(SubmissionStatus status, String judgeResult) {
        switch (status) {
            case WRONG_ANSWER:
                return "출력 결과가 예상 결과와 다릅니다.";
            case COMPILE_ERROR:
                return "컴파일 오류가 발생했습니다.";
            case RUNTIME_ERROR:
                return "실행 중 오류가 발생했습니다.";
            case TIME_LIMIT_EXCEEDED:
                return "시간 제한을 초과했습니다.";
            case MEMORY_LIMIT_EXCEEDED:
                return "메모리 제한을 초과했습니다.";
            default:
                return judgeResult;
        }
    }
    
    /**
     * 특정 문제의 특정 회원의 제출 기록 조회
     */
    public List<ProblemSubmission> getMemberSubmissions(Long problemId, Long memberId) {
        return submissionRepository.findByProblemAndMemberOrderBySubmissionTimeDesc(problemId, memberId);
    }
    
    /**
     * 특정 문제의 모든 제출 기록 조회
     */
    public List<ProblemSubmission> getProblemSubmissions(Long problemId) {
        return submissionRepository.findByProblemOrderBySubmissionTimeDesc(problemId);
    }
    
    /**
     * 특정 회원의 모든 제출 기록 조회
     */
    public List<ProblemSubmission> getMemberAllSubmissions(Long memberId) {
        return submissionRepository.findByMemberOrderBySubmissionTimeDesc(memberId);
    }
    
    /**
     * 특정 회원이 특정 문제를 이미 해결했는지 확인
     */
    public boolean hasSolved(Long problemId, Long memberId) {
        return submissionRepository.existsAcceptedSubmission(problemId, memberId);
    }
    
    /**
     * 특정 문제에 대한 특정 회원의 제출 횟수
     */
    public int getSubmissionCount(Long problemId, Long memberId) {
        return submissionRepository.countSubmissionsByProblemAndMember(problemId, memberId);
    }
    
    /**
     * 제출 기록 상세 조회
     */
    public Optional<ProblemSubmission> getSubmissionById(Long submissionId) {
        return submissionRepository.findByIdWithFetch(submissionId);
    }
}
