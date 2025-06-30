package com.example.kokkiri.admin.dto;

import com.example.kokkiri.member.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMemberManageReqDto {
    
    private Role role;          // 변경할 권한 (USER, ADMIN)
    private String isActive;    // 계정 상태 (Y: 활성화, N: 제한)
}
