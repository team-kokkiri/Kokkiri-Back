package com.example.kokkiri.report.controller;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.report.domain.ReportReason;
import com.example.kokkiri.report.domain.ReportStatus;
import com.example.kokkiri.report.dto.ReportListResDto;
import com.example.kokkiri.report.dto.ReportReqDto;
import com.example.kokkiri.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    // 신고 하기
    @PostMapping
    public ResponseEntity<Void> reportContent(@RequestBody ReportReqDto reportReqDto,
                                              @AuthenticationPrincipal Member reporter) {
        reportService.saveReport(reportReqDto, reporter);
        return ResponseEntity.ok().build();
    }

    // 신고 사유 보여주기
    @GetMapping("/reasons")
    public ResponseEntity<List<Map<String, String>>> getReportReasons() {
        List<Map<String, String>> reasons = Arrays.stream(ReportReason.values())
                .map(reason -> Map.of(
                        "code", reason.name(),
                        "description", reason.getDescription()
                ))
                .toList();
        return ResponseEntity.ok(reasons);
    }

    // 신고 리스트 조회 (status 필터링)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportListResDto>> getReports(@RequestParam(defaultValue = "PENDING") ReportStatus status) {
        List<ReportListResDto> reports = reportService.getReportList(status);
        return ResponseEntity.ok(reports);
    }

    // 상태 변경 (예: 관리자 처리 완료 등)
    @PatchMapping("/{reportId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateStatus(@PathVariable Long reportId, @RequestParam ReportStatus status) {
        reportService.updateReportStatus(reportId, status);
    }


}
