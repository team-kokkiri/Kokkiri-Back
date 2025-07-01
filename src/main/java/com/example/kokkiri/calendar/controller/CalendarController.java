package com.example.kokkiri.calendar.controller;

import com.example.kokkiri.calendar.dto.CalendarCreateRequestDto;
import com.example.kokkiri.calendar.dto.CalendarResponseDto;
import com.example.kokkiri.calendar.dto.CalendarUpdateRequestDto;
import com.example.kokkiri.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    // 일정 생성 API
    @PostMapping
    public ResponseEntity<CalendarResponseDto> createCalendar(
            @RequestBody CalendarCreateRequestDto dto,
            @RequestParam Long memberId  // 추후 시큐리티 로그인 세션에서 꺼내오기
    ) {
        CalendarResponseDto responseDto = calendarService.createCalendar(dto, memberId);
        return ResponseEntity.ok(responseDto);
    }

    // 회원별 개인 + 공용 일정 조회 API (기간 조건 필수)
    @GetMapping("/user")
    public ResponseEntity<List<CalendarResponseDto>> getUserCalendarsWithPublic(
            @RequestParam Long memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<CalendarResponseDto> calendars = calendarService.getUserCalendarsWithPublic(memberId, startDate, endDate);
        return ResponseEntity.ok(calendars);
    }

    //수정
    @PatchMapping("/{calendarId}")
    public ResponseEntity<CalendarResponseDto> updateCalendar(
            @PathVariable Long calendarId,
            @RequestBody CalendarUpdateRequestDto dto,
            @RequestParam Long memberId
    ) {
        CalendarResponseDto updated = calendarService.updateCalendar(calendarId, dto, memberId);
        return ResponseEntity.ok(updated);
    }

    //삭제
    @DeleteMapping("/{calendarId}")
    public ResponseEntity<Void> deleteCalendar(
            @PathVariable Long calendarId,
            @RequestParam Long memberId // 실제론 인증 정보에서 꺼내는게 베스트
    ) {
        calendarService.deleteCalendar(calendarId, memberId);
        return ResponseEntity.ok().build();
    }
}
