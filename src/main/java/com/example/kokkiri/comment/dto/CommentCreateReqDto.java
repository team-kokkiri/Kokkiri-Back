package com.example.kokkiri.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateReqDto {
    private Long boardId;
    private Long memberId;
    //    private Long parentId;
    private String comment;
}
