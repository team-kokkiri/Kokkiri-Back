package com.example.kokkiri.member.domain;

import com.example.kokkiri.calendar.domain.CalendarEvent;
import com.example.kokkiri.member.Role;
import com.example.kokkiri.team.domain.Team;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member implements UserDetails {  // UserDetails 구현

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    private String provider; // OAuth2 제공자

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<CalendarEvent> calendarEvents = new ArrayList<>();


    // === UserDetails 인터페이스 메서드 구현 ===

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_ 접두사 붙여서 권한 반환 (ex. ROLE_USER, ROLE_ADMIN)
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;  // 로그인 아이디로 email 사용
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;  // 계정 만료 안 됨으로 처리
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  // 계정 잠기지 않음
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // 비밀번호 만료 안 됨
    }

    @Override
    public boolean isEnabled() {
        return isActive.equals("Y");  // 활성화 여부 체크
    }
}
