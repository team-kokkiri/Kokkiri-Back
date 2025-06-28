package com.example.kokkiri.admin.controller;

import com.example.kokkiri.admin.dto.AdminDashboardResDto;
import com.example.kokkiri.admin.dto.AdminMemberListResDto;
import com.example.kokkiri.admin.service.AdminService;
import com.example.kokkiri.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminService adminService;
    
    /**
     * 관리자 대시보드 데이터 조회
     * - 전체 가입자 수
     * - 오늘 신규 가입자 수
     * - 오늘 작성된 게시글 수
     * - 오늘 신고 건수 (추후 구현)
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    // 관리자 대시보드 데이터 조회 - 관리자 페이지에서 보여줄 통계 데이터를 조회합니다.
    public ResponseEntity<CommonResDto> getDashboardData() {
        try {
            AdminDashboardResDto dashboardData = adminService.getDashboardData();
            
            CommonResDto resDto = new CommonResDto(HttpStatus.OK, "관리자 대시보드 데이터 조회 성공", dashboardData);
                    
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (Exception e) {
            CommonResDto resDto = new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "관리자 대시보드 데이터 조회 실패: " + e.getMessage(), null);
                    
            return new ResponseEntity<>(resDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 관리자용 전체 회원 목록 조회 (본인 제외)
     * - 페이징 지원
     * - 최신 가입순 정렬
     */
    @GetMapping("/members")
    @PreAuthorize("hasRole('ADMIN')")
    // 관리자용 전체 회원 목록 조회 - 본인을 제외한 전체 회원 목록을 페이징으로 조회합니다.
    public ResponseEntity<CommonResDto> getAllMembers(
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        try {
            Page<AdminMemberListResDto> membersPage = adminService.getAllMembers(pageable);
            
            CommonResDto resDto = new CommonResDto(HttpStatus.OK, "회원 목록 조회 성공", membersPage);
                    
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (Exception e) {
            CommonResDto resDto = new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "회원 목록 조회 실패: " + e.getMessage(), null);
                    
            return new ResponseEntity<>(resDto, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
