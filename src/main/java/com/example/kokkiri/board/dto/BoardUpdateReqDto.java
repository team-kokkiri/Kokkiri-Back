package com.example.kokkiri.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BoardUpdateReqDto {
    private String boardTitle;
    private String boardContent;
    private List<Long> keepFileIds; // 남아있는 첨부파일
}
