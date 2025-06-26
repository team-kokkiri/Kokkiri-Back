package com.example.kokkiri.calendar.controller;

import com.example.kokkiri.calendar.dto.CalendarCreateRequestDto;
import com.example.kokkiri.calendar.dto.CalendarResponseDto;
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
}
