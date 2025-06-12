package com.example.kokkiri.member.domain;

import com.example.kokkiri.calendar.domain.CalendarEvent;
import com.example.kokkiri.group.domain.Group;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    private String provider; // OAuth2 제공자

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String delYn = "N";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<CalendarEvent> calendarEvents = new ArrayList<>();


    // 양방향 매핑이 필요하다면 아래 주석 해제
    // @OneToMany(mappedBy = "member")
    // private List<Board> boards = new ArrayList<>();
}
