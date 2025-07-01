package com.example.kokkiri.admin.service;

import com.example.kokkiri.admin.dto.AdminDashboardResDto;
import com.example.kokkiri.admin.dto.AdminMemberDetailResDto;
import com.example.kokkiri.admin.dto.AdminMemberListResDto;
import com.example.kokkiri.admin.dto.AdminMemberManageReqDto;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.report.repository.ReportRepository;
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
    private final ReportRepository reportRepository;
    
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
        
        // 4. 오늘 신고 건수
        long todayReportCount = reportRepository.countByCreatedTimeAfter(todayStart);
        
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
        Member currentMember = memberRepository.findByEmailAndIsDeleted

(currentUserEmail,"N")
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
    
    /**
     * 특정 회원 상세 정보 조회
     * @param memberId 회원 ID
     * @return 회원 상세 정보
     */
    @Transactional(readOnly = true)
    public AdminMemberDetailResDto getMemberDetail(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));
        
        // 현재 로그인한 관리자와 동일한 회원인지 확인
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (member.getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("자기 자신의 정보는 관리할 수 없습니다.");
        }
        
        String teamName = "NO TEAM";
        try {
            if (member.getTeam() != null) {
                teamName = member.getTeam().getTeamName();
            }
        } catch (Exception e) {
            teamName = "NO TEAM";
        }
        
        return AdminMemberDetailResDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .avatar(member.getAvatar())  // 아바타 추가
                .role(member.getRole())
                .isActive(member.getIsActive())
                .teamName(teamName)
                .build();
    }
    
    /**
     * 회원 권한 변경
     * @param memberId 회원 ID
     * @param newRole 변경할 권한
     */
    @Transactional
    public void changeUserRole(Long memberId, Role newRole) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));
        
        // 현재 로그인한 관리자와 동일한 회원인지 확인
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (member.getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("자기 자신의 권한은 변경할 수 없습니다.");
        }
        
        // 권한 변경
        member.setRole(newRole);
        memberRepository.save(member);
    }
    
    /**
     * 계정 상태 변경 (활성화/비활성화)
     * @param memberId 회원 ID
     * @param isActive 계정 상태 (Y: 활성화, N: 비활성화)
     */
    @Transactional
    public void changeAccountStatus(Long memberId, String isActive) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));
        
        // 현재 로그인한 관리자와 동일한 회원인지 확인
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (member.getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("자기 자신의 계정 상태는 변경할 수 없습니다.");
        }
        
        // 입력값 검증
        if (!"Y".equals(isActive) && !"N".equals(isActive)) {
            throw new RuntimeException("잘못된 계정 상태값입니다. (Y 또는 N만 가능)");
        }
        
        // 계정 상태 변경
        member.setIsActive(isActive);
        memberRepository.save(member);
    }
    
    /**
     * 회원 종합 관리 (권한 + 계정 상태 동시 변경)
     * @param memberId 회원 ID
     * @param reqDto 변경할 정보
     */
    @Transactional
    public void manageMember(Long memberId, AdminMemberManageReqDto reqDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));
        
        // 현재 로그인한 관리자와 동일한 회원인지 확인
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (member.getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("자기 자신의 정보는 관리할 수 없습니다.");
        }
        
        // 권한 변경 (요청에 포함된 경우만)
        if (reqDto.getRole() != null) {
            member.setRole(reqDto.getRole());
        }
        
        // 계정 상태 변경 (요청에 포함된 경우만)
        if (reqDto.getIsActive() != null) {
            if (!"Y".equals(reqDto.getIsActive()) && !"N".equals(reqDto.getIsActive())) {
                throw new RuntimeException("잘못된 계정 상태값입니다. (Y 또는 N만 가능)");
            }
            member.setIsActive(reqDto.getIsActive());
        }
        
        memberRepository.save(member);
    }
}
