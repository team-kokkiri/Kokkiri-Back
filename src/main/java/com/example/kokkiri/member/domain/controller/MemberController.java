package com.example.kokkiri.member.domain.controller;

import com.example.kokkiri.member.domain.dto.MemberSignupRequest;
import com.example.kokkiri.member.domain.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody MemberSignupRequest request){
        memberService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

}
