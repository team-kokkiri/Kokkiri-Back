package com.example.kokkiri.compiler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/compiler")
public class CompilerController {

    private final WebClient webClient;

    // RapidAPI Judge0 키는 application.yml에 judge0.api-key로 설정
    public CompilerController(WebClient.Builder webClientBuilder,
                              @Value("${judge0.api-key}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://judge0-ce.p.rapidapi.com")
                .defaultHeader("x-rapidapi-key", apiKey)
                .defaultHeader("x-rapidapi-host", "judge0-ce.p.rapidapi.com")
                .build();
    }

    // 코드 템플릿 반환 (옵션)
    @GetMapping("/template")
    public Map<String, String> getDefaultTemplate() {
        String defaultCode = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
            """;
        Map<String, String> res = new HashMap<>();
        res.put("sourceCode", defaultCode);
        return res;
    }

    // 코드 실행 엔드포인트 (Judge0 + polling)
    @PostMapping(value = "/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, String>>> compileCode(@RequestBody Map<String, String> requestBody) {
        String sourceCode = requestBody.getOrDefault("sourceCode", "");
        String languageId = requestBody.getOrDefault("languageId", "62"); // Java: 62
        String stdin = requestBody.getOrDefault("stdin", "");

        String encodedSourceCode = Base64.getEncoder().encodeToString(sourceCode.getBytes());
        String encodedStdin = Base64.getEncoder().encodeToString(stdin.getBytes());

        Map<String, String> requestPayload = new HashMap<>();
        requestPayload.put("language_id", languageId);
        requestPayload.put("source_code", encodedSourceCode);
        requestPayload.put("stdin", encodedStdin);

        return this.webClient.post()
                .uri("/submissions?base64_encoded=true&wait=false")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    String token = (String) response.get("token");
                    return pollJudge0Result(token);
                })
                .map(result -> {
                    Map<String, String> responseMap = new HashMap<>();
                    StringBuilder output = new StringBuilder();

                    String stdout = (String) result.get("stdout");
                    String stderr = (String) result.get("stderr");
                    String compileOutput = (String) result.get("compile_output");
                    String message = (String) result.get("message");

                    if (stdout != null) {
                        output.append(new String(Base64.getDecoder().decode(stdout)));
                    }
                    if (stderr != null) {
                        output.append(new String(Base64.getDecoder().decode(stderr)));
                    }
                    if (compileOutput != null) {
                        output.append(new String(Base64.getDecoder().decode(compileOutput)));
                    }
                    if (message != null) {
                        output.append(message);
                    }

                    // 만약 아무런 출력도 없다면, status 설명 반환
                    if (output.length() == 0 && result.get("status") != null) {
                        Map status = (Map) result.get("status");
                        output.append("Status: ").append(status.get("description"));
                    }

                    responseMap.put("output", output.toString());
                    return ResponseEntity.ok(responseMap);
                })
                .onErrorResume(e -> {
                    Map<String, String> responseMap = new HashMap<>();
                    responseMap.put("output", "An error occurred: " + e.getMessage());
                    return Mono.just(ResponseEntity.ok(responseMap));
                });
    }

    // Polling: Judge0 결과가 '완료'될 때까지 주기적으로 체크
    private Mono<Map> pollJudge0Result(String token) {
        return Mono.defer(() -> getJudge0Result(token))
                .expand(result -> {
                    Map status = (Map) result.get("status");
                    Integer statusId = status != null ? (Integer) status.get("id") : null;
                    // 1: In Queue, 2: Processing
                    if (statusId != null && (statusId == 1 || statusId == 2)) {
                        return Mono.delay(java.time.Duration.ofMillis(800)).then(getJudge0Result(token));
                    } else {
                        return Mono.empty();
                    }
                })
                .last();
    }

    private Mono<Map> getJudge0Result(String token) {
        return this.webClient.get()
                .uri("/submissions/" + token + "?base64_encoded=true")
                .retrieve()
                .bodyToMono(Map.class);
    }
}
