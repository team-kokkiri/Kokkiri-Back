package com.example.kokkiri.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentListResDto {
    private Long id;
    private Long memberId;
    //    private Long parentId;
    private String comment;
    private LocalDateTime commentCreatedAt;
}
