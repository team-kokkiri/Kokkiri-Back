package com.example.kokkiri.member.service;

import com.example.kokkiri.common.jwt.JwtUtil;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.domain.Role;
import com.example.kokkiri.member.dto.MemberLoginReqDto;
import com.example.kokkiri.member.dto.MemberSearchResDto;
import com.example.kokkiri.member.dto.MemberSignupReqDto;
import com.example.kokkiri.member.dto.MemberInfoResDto;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.team.domain.Team;
import com.example.kokkiri.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    // ✅ 회원가입 - state 기반 teamCode 조회로 변경
    public void signup(MemberSignupReqDto request) {
        // 1. 이메일 인증 확인
        if (!emailService.isEmailVerified(request.getEmail(), "signup")) {
            throw new IllegalStateException("이메일 인증이 필요합니다.");
        }

        // 2. Redis에서 state로 teamCode 조회
        String state = request.getState();
        String teamCode = redisTemplate.opsForValue().get("state:teamCode:" + state);

        if (teamCode == null || teamCode.isBlank()) {
            throw new IllegalStateException("유효하지 않거나 만료된 state입니다.");
        }

        // 3. 팀코드 유효성 확인
        Team team = teamRepository.findByTeamCode(teamCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 팀 코드입니다."));

        // 4. 이메일 중복 체크
        if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 5. 비밀번호 유효성 검사
        isValidPassword(request.getPassword());

        // 6. 닉네임 생성
        String nickname = request.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = generateRandomNickname();
        }

        // 7. 랜덤 프로필 이미지 선택
        String randomProfileImage = DEFAULT_PROFILE_IMAGES.get(
                new Random().nextInt(DEFAULT_PROFILE_IMAGES.size()));

        // 8. 회원 저장
        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(nickname)
                .avatar(randomProfileImage)
                .team(team)
                .role(Role.USER)
                .build();

        memberRepository.save(member);

        // 9. Redis에서 사용한 state 제거 (선택)
        redisTemplate.delete("state:teamCode:" + state);
    }

    // 로그인
    public Member login(MemberLoginReqDto request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        return member;
    }

    // 비밀번호 재설정
    public void resetPassword(String email, String newPassword) {
        // 1. 이메일 인증 확인
        if (!emailService.isEmailVerified(email, "reset")) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        // 2. 회원 존재 확인
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 3. 비밀번호 암호화 후 저장
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);

        // 4. 인증 정보 삭제
        String verifiedKey = "email:verified:reset:" + email;
        emailService.deleteVerifiedKey(verifiedKey);
    }

    // 회원 검색
    public List<MemberSearchResDto> searchMember(String keyword, Long lastId, int size) {
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));
        List<Member> members = memberRepository.searchByKeyword(keyword, lastId, pageable);

        return members.stream()
                .map(m -> MemberSearchResDto.builder()
                        .memberId(m.getId())
                        .nickname(m.getNickname())
                        .email(m.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

//    // 내 정보 조회용 DTO 반환
//    public MemberInfoResDto getMyInfo(String email) {
//        Member member = memberRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//        return new MemberInfoResDto(member.getId(), member.getEmail(), member.getNickname(), member.getRole().name(),member.getAvatar());
//    }
//
//    // Access Token 발급
//    public String generateAccessToken(String email) {
//        Member member = memberRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//        return jwtUtil.generateToken(email, member.getRole().name(), true,member.getNickname(),null);
//    }

    // 비밀번호 유효성 검사
    private boolean isValidPassword(String password) {
        // 1. 공백 제거 & 길이 체크
        if (password == null || password.length() < 8 || password.length() > 32 || password.contains(" ")) {
            return false;
        }

        // 2. 문자 종류 검사
        int count = 0;
        if (password.matches(".*[A-Za-z].*")) count++;       // 영문 포함
        if (password.matches(".*\\d.*")) count++;             // 숫자 포함
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) count++; // 특수문자 포함

        if (count < 2) return false;

        // 3. 동일 문자 3자리 이상 반복 검사 (예: aaa, 111)
        if (password.matches(".*(.)\\1\\1.*")) return false;

        return true;
    }

    // 닉네임 랜덤 생성
    private String generateRandomNickname() {
        return "고라니" + ((int) (Math.random() * 100) + 1);
    }

    // 기본 프로필 이미지 URL 3개 미리 지정 (서버 정적 리소스 위치)
    private static final List<String> DEFAULT_PROFILE_IMAGES = List.of(
            "/images/profiles/default1.png",
            "/images/profiles/default2.png",
            "/images/profiles/default3.png"
    );

    @Transactional
    public void updateNickname(String email, String newNickname) {
        // 1. 새 닉네임 중복 체크
        boolean exists = memberRepository.findByNickname(newNickname).isPresent();
        if (exists) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 2. 회원 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 닉네임 변경
        member.setNickname(newNickname);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (!isValidPassword(newPassword)) {
            throw new IllegalArgumentException("비밀번호는 8~32자, 공백 제외, 영문/숫자/특수문자 중 2가지 이상 포함하고 동일문자 3번 이상 반복할 수 없습니다.");
        }

        // 새 비밀번호 암호화 후 저장
        member.setPassword(passwordEncoder.encode(newPassword));
    }
}


















