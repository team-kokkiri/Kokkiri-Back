package com.example.kokkiri.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BoardPageResDto {
    private List<BoardListResDto> boardListResDtos;
    private int currentPage; // 현재 페이지
    private int totalPages; // 총 페이지
    private long totalElements; // 전체 게시글
    private boolean isLast; // 현재 페이지가 마지막 페이지인지 여부
}
