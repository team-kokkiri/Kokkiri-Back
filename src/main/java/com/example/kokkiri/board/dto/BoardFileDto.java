package com.example.kokkiri.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class BoardFileDto {
    private Long id;
    private String fileUrl;
    private String originalName;
    private String fileType;
    private Long fileSize;
}
