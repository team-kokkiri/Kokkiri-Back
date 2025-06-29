package com.example.kokkiri.problem.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProblemPageResDto {
    
    private List<DailyProblemListResDto> problems;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public static ProblemPageResDto of(List<DailyProblemListResDto> problems, 
                                      int currentPage, 
                                      int totalPages, 
                                      long totalElements, 
                                      int size) {
        return ProblemPageResDto.builder()
                .problems(problems)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .size(size)
                .hasNext(currentPage < totalPages - 1)
                .hasPrevious(currentPage > 0)
                .build();
    }
}
