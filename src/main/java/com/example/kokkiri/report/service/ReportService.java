package com.example.kokkiri.report.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.comment.domain.Comment;
import com.example.kokkiri.comment.repository.CommentRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.report.domain.Report;
import com.example.kokkiri.report.domain.ReportStatus;
import com.example.kokkiri.report.domain.ReportType;
import com.example.kokkiri.report.dto.ReportListResDto;
import com.example.kokkiri.report.dto.ReportReqDto;
import com.example.kokkiri.report.repository.ReportRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    // 신고 하기
    public void saveReport(ReportReqDto reportReqDto, Member reporter) {
        // 1. 중복 신고 방지
        boolean isDuplicate = reportRepository.existsByReporterAndReportTypeAndTargetId(
                reporter,
                reportReqDto.getReportType(),
                reportReqDto.getTargetId()
        );
        if (isDuplicate) {
            throw new IllegalStateException("이미 신고한 콘텐츠입니다.");
        }

        // 2. 대상 존재 여부 검증
        switch (reportReqDto.getReportType()) {
            case POST -> {
                if (!boardRepository.existsById(reportReqDto.getTargetId())) {
                    throw new IllegalArgumentException("신고 대상 게시글이 존재하지 않습니다.");
                }
            }
            case COMMENT, REPLY -> {
                if (!commentRepository.existsById(reportReqDto.getTargetId())) {
                    throw new IllegalArgumentException("신고 대상 댓글이 존재하지 않습니다.");
                }
            }
        }

        // 3. 신고 저장
        Report report = Report.builder()
                .reportType(reportReqDto.getReportType())
                .targetId(reportReqDto.getTargetId())
                .reportReason(reportReqDto.getReportReason())
                .status(ReportStatus.PENDING)
                .reporter(reporter)
                .build();
        reportRepository.save(report);

        // 4. 신고 카운트 증가
        switch (reportReqDto.getReportType()) {
            case POST -> boardRepository.findById(reportReqDto.getTargetId())
                    .ifPresent(Board::increaseReportCount);
            case COMMENT, REPLY -> commentRepository.findById(reportReqDto.getTargetId())
                    .ifPresent(Comment::increaseReportCount);
        }
    }

    // 신고 리스트 조회
    public List<ReportListResDto> getReportList(ReportStatus status) {
        List<Report> reports = reportRepository.findAllByStatus(status);

        return reports.stream().map(report -> {
            String preview = getContentPreview(report.getReportType(), report.getTargetId());
            long reportCount = switch (report.getReportType()) {
                case POST -> boardRepository.findById(report.getTargetId())
                        .map(Board::getReportCount).orElse(0);
                case COMMENT, REPLY -> commentRepository.findById(report.getTargetId())
                        .map(Comment::getReportCount).orElse(0);
            };

            Long boardId = null;
            Long boardTypeId = null;

            if (report.getReportType() == ReportType.POST) {
                boardId = report.getTargetId();
                boardTypeId = boardRepository.findById(boardId)
                        .map(b -> b.getBoardType().getId())
                        .orElse(null);
            } else {
                Comment comment = commentRepository.findById(report.getTargetId()).orElse(null);
                if (comment != null) {
                    boardId = comment.getBoard().getId();
                    boardTypeId = comment.getBoard().getBoardType().getId();
                }
            }

            return ReportListResDto.builder()
                    .reportId(report.getReportId())
                    .reportType(report.getReportType().name())
                    .targetId(report.getTargetId())
                    .reportReason(report.getReportReason().name())
                    .reportCount(reportCount)
                    .status(report.getStatus().name())
                    .reporterNickname(report.getReporter().getNickname())
                    .contentPreview(preview)
                    .createdAt(report.getCreatedTime().toString())
                    .boardId(boardId)
                    .boardTypeId(boardTypeId)
                    .build();
        }).toList();
    }

    // 신고 대상 콘텐츠의 미리보기 텍스트
    private String getContentPreview(ReportType type, Long targetId) {
        return switch (type) {
            case POST -> boardRepository.findById(targetId)
                    .map(Board::getBoardContent).orElse("삭제된 게시글");
            case COMMENT, REPLY -> commentRepository.findById(targetId)
                    .map(Comment::getCommentContent).orElse("삭제된 댓글/답글");
        };
    }

    // 신고 상태 변경
    public void updateReportStatus(Long reportId, ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("ID " + reportId + "에 해당하는 신고가 존재하지 않습니다."));
        report.setStatus(status);
        reportRepository.save(report);
    }
}

