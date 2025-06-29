package com.example.kokkiri.report.controller;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.report.domain.ReportStatus;
import com.example.kokkiri.report.dto.ReportDetailResDto;
import com.example.kokkiri.report.dto.ReportReqDto;
import com.example.kokkiri.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    // 신고
    @PostMapping
    public ResponseEntity<Void> reportContent(@RequestBody ReportReqDto reportReqDto,
                                              @AuthenticationPrincipal Member reporter) {
        reportService.saveReport(reportReqDto, reporter);
        return ResponseEntity.ok().build();
    }

    // 신고 리스트 조회 (status 필터링)
    @GetMapping
    public Page<ReportDetailResDto> getReports(@RequestParam(defaultValue = "PENDING") ReportStatus status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reportService.getReportList(status, pageable);
    }

    // 상태 변경 (예: 관리자 처리 완료 등)
    @PatchMapping("/{reportId}/status")
    public void updateStatus(@PathVariable Long reportId, @RequestParam ReportStatus status) {
        reportService.updateReportStatus(reportId, status);
    }
}
