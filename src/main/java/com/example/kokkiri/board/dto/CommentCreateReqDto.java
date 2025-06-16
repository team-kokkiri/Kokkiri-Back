package com.example.kokkiri.board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateReqDto {
    private Long memberId;
    //    private Long parentId;
    private String comment;
}
