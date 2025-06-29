package com.example.kokkiri.problem.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyProblemUpdateReqDto {
    
    private String title;
    
    private String description;
    
    private String inputDescription;
    
    private String outputDescription;
    
    private String sampleInput;
    
    private String sampleOutput;
    
    private Integer timeLimit;
    
    private Integer memoryLimit;
}
