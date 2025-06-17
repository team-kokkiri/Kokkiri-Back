package com.example.kokkiri.member.service;

import com.example.kokkiri.member.Role;
import com.example.kokkiri.member.dto.MemberLoginReqDto;
import com.example.kokkiri.member.dto.MemberSignupReqDto;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.domain.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public void signup(MemberSignupReqDto request) {
        // 이메일 인증 확인
        boolean verified = emailService.isEmailVerified(request.getEmail(), "signup");
        if (!verified) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }


        Team team = teamRepository.findByTeamcode(request.getTeamCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지않은 팀코드입니다."));

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .team(team)
                .role(Role.USER)
                .build();

        memberRepository.save(member);
    }

    public Member login(MemberLoginReqDto request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다"));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }
        return member;
    }
}
