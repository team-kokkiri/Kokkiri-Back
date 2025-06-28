package com.example.kokkiri.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResDto {
    
    private Long totalMemberCount;       // 전체 가입자 수
    private Long todayNewMemberCount;    // 오늘 신규 가입자 수
    private Long todayBoardCount;        // 오늘 작성된 게시글 수
    private Long todayReportCount;       // 오늘 신고 건수 (추후 구현)
}
