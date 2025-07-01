package com.example.kokkiri.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportListResDto {
    private Long reportId;
    private String reportType;     // POST, COMMENT, REPLY
    private Long targetId;
    private String reportReason;
    private Long reportCount;
    private String status;
    private String reporterNickname;

    private Long boardId;          // COMMENT, REPLY 용 게시글 ID
    private Long boardTypeId;      // COMMENT, REPLY 용 게시판 타입 ID

    private String contentPreview; // 게시글/댓글/대댓글 일부 미리보기
    private String createdAt;      // 생성일자
}
