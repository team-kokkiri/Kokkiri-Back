package com.example.kokkiri.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BoardListResDto {
    private Long id;
    private String boardTitle;
    private String boardContent;
    private String writer;
    private int likeCount;
    private int commentCount;
    private LocalDateTime CreatedAt;
    private String boardTypes;
}