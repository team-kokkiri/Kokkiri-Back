package com.example.kokkiri.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BoardUpdateReqDto {
    private String boardTitle;
    private String boardContent;
    // 첨부파일
}
