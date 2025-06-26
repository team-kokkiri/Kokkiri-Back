package com.example.kokkiri.board.dto;

import com.example.kokkiri.board.domain.Board;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyWrittenResDto {
    private Long boardId;
    private String boardTitle;
    private LocalDateTime createdTime;
    private int likeCount;
    private int commentCount;

    public MyWrittenResDto(Board board, Long commentCount) {
        this.boardId = board.getId();
        this.boardTitle = board.getBoardTitle();
        this.createdTime = board.getCreatedTime();
        this.likeCount = board.getLikeCount();
        this.commentCount = commentCount.intValue();
    }
}
