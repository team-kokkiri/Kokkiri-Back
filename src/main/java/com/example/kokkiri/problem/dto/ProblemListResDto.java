package com.example.kokkiri.problem.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProblemListResDto {
    
    private List<DailyProblemResDto> problems;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public static ProblemListResDto of(List<DailyProblemResDto> problems, 
                                      int currentPage, 
                                      int totalPages, 
                                      long totalElements, 
                                      int size, 
                                      boolean first, 
                                      boolean last, 
                                      boolean hasNext, 
                                      boolean hasPrevious) {
        return ProblemListResDto.builder()
                .problems(problems)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .size(size)
                .first(first)
                .last(last)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
}
