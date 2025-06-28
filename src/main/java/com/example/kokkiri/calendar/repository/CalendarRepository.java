package com.example.kokkiri.calendar.repository;

import com.example.kokkiri.calendar.domain.CalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarRepository extends JpaRepository<CalendarEntity, Long> {

    // 기간 내 공용 일정 조회
    List<CalendarEntity> findByIsPublicTrueAndDateBetween(LocalDate startDate, LocalDate endDate);

    // 특정 멤버의 개인 일정 조회 (기간 내)
    List<CalendarEntity> findByMemberIdAndIsPublicFalseAndDateBetween(Long memberId, LocalDate startDate, LocalDate endDate);

    // 특정 멤버의 모든 개인 일정 조회
    List<CalendarEntity> findByMemberIdAndIsPublicFalse(Long memberId);
}