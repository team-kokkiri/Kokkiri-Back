package com.example.kokkiri.member.domain.controller;

import com.example.kokkiri.common.domain.jwt.JwtResponse;
import com.example.kokkiri.common.domain.jwt.JwtUtil;
import com.example.kokkiri.member.domain.dto.MemberInfoResponse;
import com.example.kokkiri.member.domain.dto.MemberLoginRequest;
import com.example.kokkiri.member.domain.dto.MemberSignupRequest;
import com.example.kokkiri.member.domain.entity.Member;
import com.example.kokkiri.member.domain.repository.MemberRepository;
import com.example.kokkiri.member.domain.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody @Valid MemberSignupRequest request){
        memberService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid MemberLoginRequest request){
        try{
            Member member = memberService.login(request);

            String token = jwtUtil.generateToken(member.getEmail());

            JwtResponse jwtResponse = new JwtResponse(token, member.getEmail());

            // JwtResponse DTO를 JSON 형태로 응답
            return ResponseEntity.ok(jwtResponse);
        }catch(IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 정보가 없습니다.");
        }
        String email = authentication.getName();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        MemberInfoResponse response = new MemberInfoResponse(
                member.getEmail(),
                member.getNickname(),
                member.getRole().name()
        );
        return ResponseEntity.ok(response);
    }
}