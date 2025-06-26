package com.example.kokkiri.calendar.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarCreateRequestDto {
    private String title;
    private String description;
    private LocalDate date;
    private Boolean isPublic;
}
