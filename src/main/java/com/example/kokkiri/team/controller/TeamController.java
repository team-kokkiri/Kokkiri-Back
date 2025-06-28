package com.example.kokkiri.team.controller;

import com.example.kokkiri.team.repository.TeamRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamRepository teamRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // 1. 반 코드 유효성 검증
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verifyTeamCode(@RequestParam("code") String teamCode) {
        boolean exists = teamRepository.findByTeamCode(teamCode).isPresent();
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", exists);
        return ResponseEntity.ok(response);
    }

    // 2. 반 코드 state 저장
    @PostMapping("/state")
    public ResponseEntity<Map<String, String>> generateStateFromTeamCode(@RequestBody Map<String, String> body) {
        String teamCode = body.get("teamCode");
        if (teamCode == null || teamCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "teamCode는 필수입니다."));
        }

        boolean exists = teamRepository.findByTeamCode(teamCode).isPresent();
        if (!exists) {
            return ResponseEntity.badRequest().body(Map.of("error", "팀 코드가 유효하지 않습니다."));
        }

        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("state:teamCode:" + state, teamCode, Duration.ofMinutes(10));
        log.info("Redis에 저장: key=state:teamCode:{}, value={}", state, teamCode);

        return ResponseEntity.ok(Map.of("state", state));
    }

}
