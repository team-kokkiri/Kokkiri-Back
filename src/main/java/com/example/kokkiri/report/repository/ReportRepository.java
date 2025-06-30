package com.example.kokkiri.report.repository;

import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.report.domain.Report;
import com.example.kokkiri.report.domain.ReportStatus;
import com.example.kokkiri.report.domain.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // 신고 리스트 조회
    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    // 중복 신고 방지
    boolean existsByReporterAndReportTypeAndTargetId(Member reporter, ReportType reportType, Long targetId);

    // 신고 횟수 조회
//    long countByReportTypeAndTargetId(ReportType reportType, Long targetId);
}
