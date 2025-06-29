package com.example.kokkiri.report.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.comment.domain.Comment;
import com.example.kokkiri.comment.repository.CommentRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.report.domain.Report;
import com.example.kokkiri.report.domain.ReportStatus;
import com.example.kokkiri.report.domain.ReportType;
import com.example.kokkiri.report.dto.ReportDetailResDto;
import com.example.kokkiri.report.dto.ReportReqDto;
import com.example.kokkiri.report.repository.ReportRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    // 신고 하기
    public void saveReport(ReportReqDto reportReqDto, Member reporter) {
        Report report = Report.builder()
                .reportType(reportReqDto.getReportType())
                .targetId(reportReqDto.getTargetId())
                .reasonCode(reportReqDto.getReasonCode())
                .status(ReportStatus.PENDING)
                .reporter(reporter)
                .build();

        reportRepository.save(report);
    }

    // 신고 리스트 조회
    public Page<ReportDetailResDto> getReportList(ReportStatus status, Pageable pageable) {
        Page<Report> reports = reportRepository.findAllByStatus(status, pageable);

        return reports.map(report -> {
            String preview = getContentPreview(report.getReportType(), report.getTargetId());
            return ReportDetailResDto.builder()
                    .reportId(report.getReportId())
                    .reportType(String.valueOf(report.getReportType()))
                    .targetId(report.getTargetId())
                    .reasonCode(report.getReasonCode())
                    .status(report.getStatus().name()) // Enum을 문자열로 변환
                    .reporterNickname(report.getReporter().getNickname())
                    .contentPreview(preview)
                    .createdAt(report.getCreatedTime().toString())
                    .build();
        });
    }

    // 신고 대상 콘텐츠의 미리보기 텍스트
    private String getContentPreview(ReportType type, Long targetId) {
        return switch (type) {
            case POST -> boardRepository.findById(targetId)
                    .map(Board::getBoardContent).orElse("삭제된 게시글");
            case COMMENT, REPLY -> commentRepository.findById(targetId)
                    .map(Comment::getCommentContent).orElse("삭제된 댓글/답글");
            default -> "(알 수 없음)";
        };
    }

    // 신고 상태 병경
    public void updateReportStatus(Long reportId, ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("신고 없음"));

        report.setStatus(status);
    }
}

