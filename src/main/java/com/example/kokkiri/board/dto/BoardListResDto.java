package com.example.kokkiri.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class BoardListResDto {
    private Long id;
    private String boardTitle;
    private String boardContent;
    private String writer;
    private int likeCount;
    private Long commentCount; // count() 쿼리 결과는 JPA가 항상 Long을 반환
    private LocalDateTime createdAt;
    private String boardType;
    private String thumbnailUrl;
}