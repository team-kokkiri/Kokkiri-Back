package com.example.kokkiri.calendar.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendarUpdateRequestDto {
    private String title;
    private String description;
    private LocalDate date;
}
