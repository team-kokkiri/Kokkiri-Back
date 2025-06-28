package com.example.kokkiri.admin.dto;

import com.example.kokkiri.member.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMemberListResDto {
    
    private Long id;
    private String email;
    private String nickname;
    private Role role;
    private String isActive;
    private String avatar;
    private String teamName;      // 팀 이름
    private LocalDateTime createdTime;  // 가입일
}
