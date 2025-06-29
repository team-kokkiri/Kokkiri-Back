package com.example.kokkiri.report.repository;

import com.example.kokkiri.report.domain.Report;
import com.example.kokkiri.report.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);
}
