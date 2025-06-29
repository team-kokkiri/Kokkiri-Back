package com.example.kokkiri.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetailResDto {
    private Long reportId;
    private String reportType;     // POST, COMMENT, REPLY
    private Long targetId;
    private String reasonCode;
    private String status;
    private String reporterNickname;

    private String contentPreview; // 게시글/댓글/대댓글 일부 미리보기
    private String createdAt;      // 생성일자
}
