package com.example.kokkiri.problem.controller;

import com.example.kokkiri.common.dto.CommonResDto;
import com.example.kokkiri.problem.dto.DailyRankingResDto;
import com.example.kokkiri.problem.service.DailyRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
@Slf4j
public class DailyRankingController {
    
    private final DailyRankingService rankingService;
    
    /**
     * 오늘 문제 랭킹 조회 (빠르게 푼 순서대로)
     */
    @GetMapping("/today")
    public ResponseEntity<CommonResDto> getTodayRanking() {
        try {
            List<DailyRankingResDto> rankings = rankingService.getTodayRanking()
                    .stream()
                    .map(DailyRankingResDto::from)
                    .toList();
            
            log.info("오늘 문제 랭킹 조회 완료: {} 건", rankings.size());
            return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "오늘 문제 랭킹 조회 성공", rankings));
            
        } catch (Exception e) {
            log.error("오늘 문제 랭킹 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(HttpStatus.INTERNAL_SERVER_ERROR, "랭킹 조회 중 오류가 발생했습니다.", null));
        }
    }
}
