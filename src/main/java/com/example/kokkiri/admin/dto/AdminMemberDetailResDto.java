package com.example.kokkiri.admin.dto;

import com.example.kokkiri.member.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMemberDetailResDto {
    
    private Long id;
    private String email;
    private String nickname;
    private Role role;
    private String isActive;
    private String teamName;
}
