package com.example.kokkiri.problem.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProblemSubmissionReqDto {
    
    private Long problemId;
    
    private String sourceCode;
    
    private String language = "JAVA";
}
