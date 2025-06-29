package com.example.kokkiri.problem.scheduler;

import com.example.kokkiri.problem.domain.DailyProblem;
import com.example.kokkiri.problem.repository.DailyProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyProblemScheduler {
    
    private final DailyProblemRepository dailyProblemRepository;
    
    // 매일 오전 8시 30분에 실행
    @Scheduled(cron = "0 30 8 * * *")
    public void createDailyProblem() {
        LocalDate today = LocalDate.now();
        
        // 이미 오늘 문제가 있는지 확인
        if (dailyProblemRepository.existsByProblemDate(today)) {
            log.info("오늘({}) 문제가 이미 존재합니다.", today);
            return;
        }
        
        // 새로운 문제 생성
        DailyProblem newProblem = createRandomProblem(today);
        dailyProblemRepository.save(newProblem);
        
        log.info("새로운 일일 문제가 생성되었습니다: {} - {}", today, newProblem.getTitle());
    }
    
    private DailyProblem createRandomProblem(LocalDate date) {
        List<ProblemTemplate> templates = Arrays.asList(
            new ProblemTemplate(
                "두 수의 합",
                "두 정수 A와 B를 입력받은 다음, A+B를 출력하는 프로그램을 작성하시오.",
                "첫째 줄에 A와 B가 주어진다. (0 < A, B < 10)",
                "첫째 줄에 A+B를 출력한다.",
                "1 2",
                "3"
            ),
            new ProblemTemplate(
                "두 수의 곱",
                "두 정수 A와 B를 입력받은 다음, A×B를 출력하는 프로그램을 작성하시오.",
                "첫째 줄에 A와 B가 주어진다. (0 < A, B < 10)",
                "첫째 줄에 A×B를 출력한다.",
                "3 4",
                "12"
            ),
            new ProblemTemplate(
                "세 수의 최댓값",
                "세 정수 A, B, C를 입력받은 다음, 가장 큰 수를 출력하는 프로그램을 작성하시오.",
                "첫째 줄에 A, B, C가 주어진다. (1 ≤ A, B, C ≤ 100)",
                "첫째 줄에 가장 큰 수를 출력한다.",
                "3 1 4",
                "4"
            ),
            new ProblemTemplate(
                "짝수 판별",
                "정수 N을 입력받은 다음, N이 짝수이면 'EVEN', 홀수이면 'ODD'를 출력하는 프로그램을 작성하시오.",
                "첫째 줄에 정수 N이 주어진다. (1 ≤ N ≤ 100)",
                "N이 짝수이면 'EVEN', 홀수이면 'ODD'를 출력한다.",
                "4",
                "EVEN"
            ),
            new ProblemTemplate(
                "1부터 N까지의 합",
                "정수 N을 입력받은 다음, 1부터 N까지의 합을 출력하는 프로그램을 작성하시오.",
                "첫째 줄에 정수 N이 주어진다. (1 ≤ N ≤ 100)",
                "첫째 줄에 1부터 N까지의 합을 출력한다.",
                "5",
                "15"
            )
        );
        
        // 랜덤으로 문제 선택
        Random random = new Random();
        ProblemTemplate template = templates.get(random.nextInt(templates.size()));
        
        return DailyProblem.builder()
                .problemDate(date)
                .title(template.title)
                .description(template.description)
                .inputDescription(template.inputDescription)
                .outputDescription(template.outputDescription)
                .sampleInput(template.sampleInput)
                .sampleOutput(template.sampleOutput)
                .timeLimit(1000)
                .memoryLimit(128)
                .isActive("Y")
                .build();
    }
    
    // 문제 템플릿 내부 클래스
    private static class ProblemTemplate {
        final String title;
        final String description;
        final String inputDescription;
        final String outputDescription;
        final String sampleInput;
        final String sampleOutput;
        
        ProblemTemplate(String title, String description, String inputDescription,
                       String outputDescription, String sampleInput, String sampleOutput) {
            this.title = title;
            this.description = description;
            this.inputDescription = inputDescription;
            this.outputDescription = outputDescription;
            this.sampleInput = sampleInput;
            this.sampleOutput = sampleOutput;
        }
    }
}
