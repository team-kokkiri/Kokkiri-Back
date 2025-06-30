package com.example.kokkiri.report.dto;

import com.example.kokkiri.report.domain.ReportReason;
import com.example.kokkiri.report.domain.ReportType;
import lombok.Getter;

@Getter
public class ReportReqDto {
    private ReportType reportType;
    private Long targetId;
    private ReportReason reportReason;
}
