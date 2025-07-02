package com.example.kokkiri.problem.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseDto {
    private Long id;
    private String input;
    private String expectedOutput;
    private Integer orderNum;
    private Boolean isHidden;
}
