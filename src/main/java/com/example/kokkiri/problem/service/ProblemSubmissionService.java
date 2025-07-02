package com.example.kokkiri.problem.service;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.domain.ProblemSubmission;
import com.example.kokkiri.problem.domain.SubmissionStatus;
import com.example.kokkiri.problem.domain.TestCase;
import com.example.kokkiri.problem.dto.TestCaseResultDto;
import com.example.kokkiri.problem.repository.DailyProblemRepository;
import com.example.kokkiri.problem.repository.ProblemSubmissionRepository;
import com.example.kokkiri.problem.repository.TestCaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ProblemSubmissionService {
    
    private final ProblemSubmissionRepository submissionRepository;
    private final DailyProblemRepository dailyProblemRepository;
    private final MemberRepository memberRepository;
    private final TestCaseRepository testCaseRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // DailyRankingService는 @Lazy로 주입하여 순환 참조 방지
    @Lazy
    private final DailyRankingService dailyRankingService;
    
    public ProblemSubmissionService(ProblemSubmissionRepository submissionRepository,
                                  DailyProblemRepository dailyProblemRepository,
                                  MemberRepository memberRepository,
                                  TestCaseRepository testCaseRepository,
                                  @Lazy DailyRankingService dailyRankingService,
                                  WebClient.Builder webClientBuilder,
                                  @Value("${judge0.api-key}") String apiKey) {
        this.submissionRepository = submissionRepository;
        this.dailyProblemRepository = dailyProblemRepository;
        this.memberRepository = memberRepository;
        this.testCaseRepository = testCaseRepository;
        this.dailyRankingService = dailyRankingService;
        this.objectMapper = new ObjectMapper();
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
        
        // 테스트케이스 조회
        List<TestCase> testCases = testCaseRepository.findByDailyProblemIdOrderByOrderNum(problemId);
        
        // 모든 테스트케이스에 대해 Judge0 실행
        return executeJudgingWithTestCases(fetchedSubmission, problem, testCases)
                .doOnError(error -> log.error("채점 처리 중 오류 발생: 제출ID={}", fetchedSubmission.getId(), error));
    }
    
    /**
     * 모든 테스트케이스에 대한 채점 실행
     */
    private Mono<ProblemSubmission> executeJudgingWithTestCases(ProblemSubmission submission, 
                                                                DailyProblem problem, 
                                                                List<TestCase> testCases) {
        List<Mono<TestCaseResultDto>> testCaseMonos = new ArrayList<>();
        
        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            testCaseMonos.add(executeTestCase(submission, testCase, i + 1));
        }
        
        return Mono.zip(testCaseMonos, results -> {
            List<TestCaseResultDto> testCaseResults = new ArrayList<>();
            int passedCount = 0;
            SubmissionStatus finalStatus = SubmissionStatus.ACCEPTED;
            
            for (Object result : results) {
                TestCaseResultDto testCaseResult = (TestCaseResultDto) result;
                testCaseResults.add(testCaseResult);
                
                if (testCaseResult.getPassed()) {
                    passedCount++;
                } else {
                    // 첫 번째 실패한 테스트케이스의 상태를 최종 상태로 사용
                    if (finalStatus == SubmissionStatus.ACCEPTED) {
                        finalStatus = mapTestCaseStatus(testCaseResult.getStatus());
                    }
                }
            }
            
            // 제출 결과 업데이트
            submission.setPassedTestCaseCount(passedCount);
            submission.setTestCaseResults(convertToJson(testCaseResults));
            submission.setStatus(finalStatus);
            submission.setJudgeTime(LocalDateTime.now());
            
            // 모든 테스트케이스를 통과한 경우에만 랭킹 업데이트
            if (passedCount == testCases.size()) {
                submission.setStatus(SubmissionStatus.ACCEPTED);
                try {
                    dailyRankingService.updateRanking(submission);
                } catch (Exception e) {
                    log.error("랭킹 업데이트 실패: 제출ID={}", submission.getId(), e);
                }
            }
            
            return submissionRepository.save(submission);
        });
    }
    
    /**
     * 단일 테스트케이스 실행
     */
    private Mono<TestCaseResultDto> executeTestCase(ProblemSubmission submission, TestCase testCase, int testCaseNum) {
        String encodedSourceCode = Base64.getEncoder().encodeToString(submission.getSourceCode().getBytes());
        String encodedInput = Base64.getEncoder().encodeToString(testCase.getInput().getBytes());
        
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
                    return pollJudgeResult(token);
                })
                .map(result -> evaluateTestCaseResult(result, testCase, testCaseNum))
                .onErrorReturn(TestCaseResultDto.builder()
                        .testCaseNum(testCaseNum)
                        .passed(false)
                        .status("ERROR")
                        .errorMessage("테스트케이스 실행 중 오류 발생")
                        .build());
    }
    
    /**
     * 테스트케이스 결과 평가
     */
    private TestCaseResultDto evaluateTestCaseResult(Map judgeResult, TestCase testCase, int testCaseNum) {
        TestCaseResultDto.TestCaseResultDtoBuilder resultBuilder = TestCaseResultDto.builder()
                .testCaseNum(testCaseNum)
                .expectedOutput(testCase.getExpectedOutput());
        
        try {
            String stdout = (String) judgeResult.get("stdout");
            String stderr = (String) judgeResult.get("stderr");
            Map status = (Map) judgeResult.get("status");
            Object time = judgeResult.get("time");
            Object memory = judgeResult.get("memory");
            
            if (time != null) {
                resultBuilder.executionTime((int) (Double.parseDouble(time.toString()) * 1000));
            }
            if (memory != null) {
                resultBuilder.memoryUsage(Integer.parseInt(memory.toString()));
            }
            
            Integer statusId = status != null ? (Integer) status.get("id") : null;
            
            if (statusId != null && statusId == 3) { // Accepted
                if (stdout != null) {
                    String actualOutput = new String(Base64.getDecoder().decode(stdout)).trim();
                    resultBuilder.actualOutput(actualOutput);
                    
                    boolean passed = actualOutput.equals(testCase.getExpectedOutput().trim());
                    resultBuilder.passed(passed);
                    resultBuilder.status(passed ? "PASSED" : "FAILED");
                } else {
                    resultBuilder.passed(false);
                    resultBuilder.status("FAILED");
                    resultBuilder.errorMessage("출력이 없습니다");
                }
            } else {
                resultBuilder.passed(false);
                resultBuilder.status(mapStatusIdToString(statusId));
                
                if (stderr != null) {
                    resultBuilder.errorMessage(new String(Base64.getDecoder().decode(stderr)));
                }
            }
            
        } catch (Exception e) {
            log.error("테스트케이스 결과 평가 중 오류: testCase={}", testCaseNum, e);
            resultBuilder.passed(false);
            resultBuilder.status("ERROR");
            resultBuilder.errorMessage("결과 평가 중 오류 발생");
        }
        
        return resultBuilder.build();
    }
    
    /**
     * Judge0 결과 폴링
     */
    private Mono<Map> pollJudgeResult(String token) {
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
                .last();
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
    
    private String convertToJson(List<TestCaseResultDto> results) {
        try {
            return objectMapper.writeValueAsString(results);
        } catch (Exception e) {
            log.error("JSON 변환 실패", e);
            return "[]";
        }
    }
    
    private SubmissionStatus mapTestCaseStatus(String status) {
        switch (status) {
            case "FAILED":
                return SubmissionStatus.WRONG_ANSWER;
            case "COMPILE_ERROR":
                return SubmissionStatus.COMPILE_ERROR;
            case "RUNTIME_ERROR":
                return SubmissionStatus.RUNTIME_ERROR;
            case "TIME_LIMIT_EXCEEDED":
                return SubmissionStatus.TIME_LIMIT_EXCEEDED;
            default:
                return SubmissionStatus.RUNTIME_ERROR;
        }
    }
    
    private String mapStatusIdToString(Integer statusId) {
        if (statusId == null) return "ERROR";
        
        switch (statusId) {
            case 4: return "FAILED";
            case 5: return "TIME_LIMIT_EXCEEDED";
            case 6: return "COMPILE_ERROR";
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12: return "RUNTIME_ERROR";
            default: return "ERROR";
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
     * 테스트케이스 결과 파싱
     */
    public List<TestCaseResultDto> parseTestCaseResults(String testCaseResults) {
        if (testCaseResults == null || testCaseResults.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return Arrays.asList(objectMapper.readValue(testCaseResults, TestCaseResultDto[].class));
        } catch (Exception e) {
            log.error("테스트케이스 결과 파싱 실패", e);
            return new ArrayList<>();
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
