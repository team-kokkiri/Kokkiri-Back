package com.example.kokkiri.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentListResDto {
    private Long id;
    private Long memberId;
    private String memberNickname;
    private Long parentId;
    private String comment;
    private int likeCount;
    private boolean isDeleted;
    private LocalDateTime commentCreatedAt;
}
