package com.example.kokkiri.board.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardCreateReqDto {
    private Long memberId;
    private Long boardTypeId;
    private String boardTitle;
    private String boardContent;
}
