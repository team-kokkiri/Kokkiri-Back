package com.example.kokkiri.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.xml.stream.events.Comment;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class BoardDetailResDto {
    private Long id;
    private String title;
    private String content;
    private String writer;
    private String boardType;
    private int likeCount;
    private LocalDateTime boardCreatedAt;


    private List<Comment> comments;
    private LocalDateTime commentCreatedAt;

}
