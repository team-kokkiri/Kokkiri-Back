package com.example.kokkiri.problem.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class DailyProblemCreateReqDto {
    
    private LocalDate problemDate;
    
    private String title;
    
    private String description;
    
    private String inputDescription;
    
    private String outputDescription;
    
    private String sampleInput;
    
    private String sampleOutput;
    
    private Integer timeLimit = 1000;
    
    private Integer memoryLimit = 128;
}
