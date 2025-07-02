package com.example.kokkiri.problem.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseResultDto {
    private Integer testCaseNum;
    private Boolean passed;
    private String status;  // "PASSED", "FAILED", "ERROR"
    private String actualOutput;
    private String expectedOutput;
    private String errorMessage;
    private Integer executionTime;
    private Integer memoryUsage;
}
