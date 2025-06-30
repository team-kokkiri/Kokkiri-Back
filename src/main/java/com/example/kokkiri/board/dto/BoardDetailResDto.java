package com.example.kokkiri.board.dto;

import com.example.kokkiri.comment.dto.CommentListResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class BoardDetailResDto {
    private Long id;
    private String boardTitle;
    private String boardContent;
    private String writer;
    private int likeCount;
    private int commentCount;
    private LocalDateTime boardCreatedAt;
    private List<CommentListResDto> comments;
    private List<String> fileUrls;  // 기존 호환성을 위해 유지
    private List<BoardFileDto> files;  // 파일 상세 정보 추가
}
