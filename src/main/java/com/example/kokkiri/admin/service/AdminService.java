package com.example.kokkiri.admin.service;

import com.example.kokkiri.admin.dto.AdminDashboardResDto;
import com.example.kokkiri.admin.dto.AdminMemberListResDto;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
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
    
    /**
     * 관리자용 전체 회원 목록 조회 (본인 제외)
     * @param pageable 페이징 정보
     * @return 회원 목록
     */
    @Transactional(readOnly = true)
    public Page<AdminMemberListResDto> getAllMembers(Pageable pageable) {
        // 현재 로그인한 관리자 정보 조회
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Member currentMember = memberRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("로그인한 사용자를 찾을 수 없습니다."));
        
        // 본인을 제외한 전체 회원 목록 조회
        Page<Member> membersPage = memberRepository.findAllMembersExceptCurrent(currentMember.getId(), pageable);
        
        // Member 엔티티를 AdminMemberListResDto로 변환
        return membersPage.map(member -> {
            // 트랜잭션 내에서 지연 로딩 가능
            String teamName = "NO TEAM";
            try {
                if (member.getTeam() != null) {
                    teamName = member.getTeam().getTeamName();  // getName() -> getTeamName()
                }
            } catch (Exception e) {
                // 지연 로딩 실패 시 기본값 사용
                teamName = "NO TEAM";
            }
            
            return AdminMemberListResDto.builder()
                    .id(member.getId())
                    .email(member.getEmail())
                    .nickname(member.getNickname())
                    .role(member.getRole())
                    .isActive(member.getIsActive())
                    .avatar(member.getAvatar())
                    .teamName(teamName)
                    .createdTime(member.getCreatedTime())
                    .build();
        });
    }
}
