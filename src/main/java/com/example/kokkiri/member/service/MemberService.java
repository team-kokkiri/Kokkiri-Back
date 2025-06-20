package com.example.kokkiri.member.service;

import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.dto.MemberLoginReqDto;
import com.example.kokkiri.member.dto.MemberSignupReqDto;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
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

        Team team = teamRepository.findByTeamCode(request.getTeamCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지않은 팀코드입니다."));

        String nickname = request.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = "Kosa" + ((int)(Math.random() * 100) + 1); // 랜덤 1~100
        }


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

    public void resetPassword(String email, String newPassword) {
        // 1. 인증 여부 확인 (Redis에 인증 완료 여부 있는지 확인)
        boolean verified = emailService.isEmailVerified(email, "reset");
        if (!verified) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        // 2. 사용자 존재 여부 확인
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 3. 비밀번호 암호화 후 저장
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        // 4. Redis 인증 정보 삭제
        String verifiedKey = "email:verified:reset:" + email;
        emailService.deleteVerifiedKey(verifiedKey);
    }
}




