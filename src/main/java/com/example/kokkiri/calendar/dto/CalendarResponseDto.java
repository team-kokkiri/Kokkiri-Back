package com.example.kokkiri.calendar.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarResponseDto {
    private Long id;
    private String title;
    private String description;
    private LocalDate date;
    private Boolean isPublic;
    private Long memberId;
    private String memberNickname;  // member 이름 또는 닉네임 등 보여주고 싶으면 추가
}
