package com.example.kokkiri.team.controller;

import com.example.kokkiri.team.repository.TeamRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamRepository teamRepository;

    // 1. 반 코드 유효성 검증
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verifyTeamCode(@RequestParam("code") String teamCode) {
        boolean exists = teamRepository.findByTeamCode(teamCode).isPresent();
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", exists);
        return ResponseEntity.ok(response);
    }

    // 2. 반 코드 세션 저장
    @PostMapping("/session")
    public ResponseEntity<?> saveTeamCodeToSession(@RequestBody Map<String, String> body, HttpSession session) {
        String teamCode = body.get("teamCode");

        if (teamCode == null || teamCode.isBlank()) {
            return ResponseEntity.badRequest().body("teamCode가 비어있습니다.");
        }

        session.setAttribute("teamCode", teamCode);
        return ResponseEntity.ok().build();
    }

    // 세션에서 teamCode 조회
    @GetMapping("/session")
    public ResponseEntity<?> getTeamCodeFromSession(HttpSession session) {
        String teamCode = (String) session.getAttribute("teamCode");

        if (teamCode == null) {
            return ResponseEntity.status(400).body("세션에 저장된 팀 코드가 없습니다.");
        }

        Map<String, String> response = new HashMap<>();
        response.put("teamCode", teamCode);
        return ResponseEntity.ok(response);
    }
}
