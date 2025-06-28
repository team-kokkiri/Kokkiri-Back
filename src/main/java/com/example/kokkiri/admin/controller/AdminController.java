package com.example.kokkiri.admin.controller;

import com.example.kokkiri.admin.dto.AdminDashboardResDto;
import com.example.kokkiri.admin.dto.AdminMemberDetailResDto;
import com.example.kokkiri.admin.dto.AdminMemberListResDto;
import com.example.kokkiri.admin.dto.AdminMemberManageReqDto;
import com.example.kokkiri.admin.service.AdminService;
import com.example.kokkiri.common.dto.CommonResDto;
import com.example.kokkiri.member.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
     * 특정 회원 상세 정보 조회
     */
    @GetMapping("/members/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    // 특정 회원의 상세 정보를 조회합니다.
    public ResponseEntity<CommonResDto> getMemberDetail(@PathVariable Long memberId) {
        try {
            AdminMemberDetailResDto memberDetail = adminService.getMemberDetail(memberId);
            
            CommonResDto resDto = new CommonResDto(HttpStatus.OK, "회원 상세 정보 조회 성공", memberDetail);
                    
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (Exception e) {
            CommonResDto resDto = new CommonResDto(HttpStatus.BAD_REQUEST, e.getMessage(), null);
                    
            return new ResponseEntity<>(resDto, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * 회원 권한 변경
     */
    @PutMapping("/members/{memberId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    // 회원의 권한을 변경합니다.
    public ResponseEntity<CommonResDto> changeUserRole(
            @PathVariable Long memberId,
            @RequestParam Role role
    ) {
        try {
            adminService.changeUserRole(memberId, role);
            
            CommonResDto resDto = new CommonResDto(HttpStatus.OK, "회원 권한 변경 성공", null);
                    
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (Exception e) {
            CommonResDto resDto = new CommonResDto(HttpStatus.BAD_REQUEST, e.getMessage(), null);
                    
            return new ResponseEntity<>(resDto, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * 계정 상태 변경 (활성화/비활성화)
     */
    @PutMapping("/members/{memberId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    // 회원의 계정 상태를 변경합니다.
    public ResponseEntity<CommonResDto> changeAccountStatus(
            @PathVariable Long memberId,
            @RequestParam String isActive
    ) {
        try {
            adminService.changeAccountStatus(memberId, isActive);
            
            String message = "Y".equals(isActive) ? "계정 활성화 성공" : "계정 비활성화 성공";
            CommonResDto resDto = new CommonResDto(HttpStatus.OK, message, null);
                    
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (Exception e) {
            CommonResDto resDto = new CommonResDto(HttpStatus.BAD_REQUEST, e.getMessage(), null);
                    
            return new ResponseEntity<>(resDto, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * 회원 종합 관리 (권한 + 계정 상태 동시 변경)
     */
    @PutMapping("/members/{memberId}/manage")
    @PreAuthorize("hasRole('ADMIN')")
    // 회원의 권한과 계정 상태를 동시에 관리합니다.
    public ResponseEntity<CommonResDto> manageMember(
            @PathVariable Long memberId,
            @RequestBody AdminMemberManageReqDto reqDto
    ) {
        try {
            adminService.manageMember(memberId, reqDto);
            
            CommonResDto resDto = new CommonResDto(HttpStatus.OK, "회원 정보 수정 성공", null);
                    
            return new ResponseEntity<>(resDto, HttpStatus.OK);
        } catch (Exception e) {
            CommonResDto resDto = new CommonResDto(HttpStatus.BAD_REQUEST, e.getMessage(), null);
                    
            return new ResponseEntity<>(resDto, HttpStatus.BAD_REQUEST);
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
