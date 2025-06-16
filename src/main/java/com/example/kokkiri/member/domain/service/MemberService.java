package com.example.kokkiri.member.domain.service;

import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.domain.config.SecurityConfig;
import com.example.kokkiri.member.domain.dto.MemberSignupRequest;
import com.example.kokkiri.member.domain.entity.Member;
import com.example.kokkiri.member.domain.repository.MemberRepository;
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

    public void signup(MemberSignupRequest request){

        System.out.println("teamCode = " + request.getTeamCode());

        Team team = teamRepository.findByTeamCode(request.getTeamCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지않은 코드입니다."));

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .team(team)
                .role(Role.USER)
                .build();

        memberRepository.save(member);
    }

}