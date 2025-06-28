package com.example.kokkiri.admin.service;

import com.example.kokkiri.admin.dto.AdminDashboardResDto;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    
    /**
     * 관리자 대시보드 데이터 조회
     * @return AdminDashboardResDto
     */
    public AdminDashboardResDto getDashboardData() {
        // 오늘 00:00:00부터의 시간 계산
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        
        // 1. 전체 가입자 수
        long totalMemberCount = memberRepository.count();
        
        // 2. 오늘 신규 가입자 수
        long todayNewMemberCount = memberRepository.countByCreatedTimeAfter(todayStart);
        
        // 3. 오늘 작성된 게시글 수
        long todayBoardCount = boardRepository.countByCreatedTimeAfter(todayStart);
        
        // 4. 오늘 신고 건수 (추후 구현 예정)
        long todayReportCount = 0L;
        
        return AdminDashboardResDto.builder()
                .totalMemberCount(totalMemberCount)
                .todayNewMemberCount(todayNewMemberCount)
                .todayBoardCount(todayBoardCount)
                .todayReportCount(todayReportCount)
                .build();
    }
}
