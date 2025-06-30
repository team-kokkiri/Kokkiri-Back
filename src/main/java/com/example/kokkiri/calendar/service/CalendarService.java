package com.example.kokkiri.calendar.service;

import com.example.kokkiri.calendar.domain.CalendarEntity;
import com.example.kokkiri.calendar.dto.CalendarCreateRequestDto;
import com.example.kokkiri.calendar.dto.CalendarResponseDto;
import com.example.kokkiri.calendar.dto.CalendarUpdateRequestDto;
import com.example.kokkiri.calendar.repository.CalendarRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarRepository calendarRepository;
    private final MemberRepository memberRepository;

    // 일정 생성 (admin/user 모두 사용, isPublic에 따라 공용/개인 결정)
    @Transactional
    public CalendarResponseDto createCalendar(CalendarCreateRequestDto dto, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버가 존재하지 않습니다"));

        // 역할에 따른 isPublic 처리
        Boolean isPublic;
        if (member.getRole() == Role.ADMIN) {
            isPublic = dto.getIsPublic(); // ADMIN은 dto의 isPublic 값 사용 가능
        } else {
            // USER는 무조건 개인일정으로 처리
            isPublic = false;
        }

        CalendarEntity entity = CalendarEntity.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .date(dto.getDate())
                .isPublic(isPublic)
                .member(member)
                .build();

        CalendarEntity saved = calendarRepository.save(entity);

        return CalendarResponseDto.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .date(saved.getDate())
                .isPublic(saved.getIsPublic())
                .memberId(member.getId())
                .build();
    }

    // 사용자 개인 일정 + 공용 일정 조회 (기간 필터)
    @Transactional(readOnly = true)
    public List<CalendarResponseDto> getUserCalendarsWithPublic(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<CalendarEntity> publicCalendars = calendarRepository.findByIsPublicTrueAndDateBetween(startDate, endDate);
        List<CalendarEntity> privateCalendars = calendarRepository.findByMemberIdAndIsPublicFalseAndDateBetween(memberId, startDate, endDate);

        // 두 리스트 합치기
        List<CalendarEntity> allCalendars = new java.util.ArrayList<>();
        allCalendars.addAll(publicCalendars);
        allCalendars.addAll(privateCalendars);

        return allCalendars.stream()
                .map(c -> CalendarResponseDto.builder()
                        .id(c.getId())
                        .title(c.getTitle())
                        .description(c.getDescription())
                        .date(c.getDate())
                        .isPublic(c.getIsPublic())
                        .memberId(c.getMember().getId())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public CalendarResponseDto updateCalendar(Long calendarId, CalendarUpdateRequestDto dto, Long memberId) {
        CalendarEntity cal = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버가 존재하지 않습니다."));

        if (Boolean.TRUE.equals(cal.getIsPublic())) {
            // 공용 일정: 관리자인 경우만 가능
            if (member.getRole() != Role.ADMIN) {
                throw new IllegalStateException("공용 일정은 관리자만 수정할 수 있습니다.");
            }
        } else {
            // 개인 일정: 본인만 가능
            if (!cal.getMember().getId().equals(memberId)) {
                throw new IllegalStateException("본인 개인 일정만 수정할 수 있습니다.");
            }
        }

        // 수정 적용
        cal.setTitle(dto.getTitle());
        cal.setDescription(dto.getDescription());
        cal.setDate(dto.getDate());

        return CalendarResponseDto.builder()
                .id(cal.getId())
                .title(cal.getTitle())
                .description(cal.getDescription())
                .date(cal.getDate())
                .isPublic(cal.getIsPublic())
                .memberId(cal.getMember().getId())
                .build();
    }

    @Transactional
    public void deleteCalendar(Long calendarId, Long memberId) {
        CalendarEntity cal = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("일정이 존재하지 않습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버가 존재하지 않습니다."));

        // 공용 일정이면 관리자만 삭제 가능
        if (Boolean.TRUE.equals(cal.getIsPublic())) {
            if (member.getRole() != Role.ADMIN) {
                throw new IllegalStateException("공용 일정은 관리자만 삭제할 수 있습니다.");
            }
        } else {
            // 개인 일정이면 본인만 삭제 가능
            if (!cal.getMember().getId().equals(memberId)) {
                throw new IllegalStateException("본인 일정만 삭제할 수 있습니다.");
            }
        }
        calendarRepository.delete(cal);
    }
}
