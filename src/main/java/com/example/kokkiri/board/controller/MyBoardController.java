package com.example.kokkiri.board.controller;

import com.example.kokkiri.board.dto.MyWrittenResDto;
import com.example.kokkiri.board.service.MyBoardService;
import com.example.kokkiri.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/myboards")
@RequiredArgsConstructor
public class MyBoardController {

    private final MyBoardService myBoardService;

    // 내가 쓴 글
    @GetMapping("/written")
    public ResponseEntity<List<MyWrittenResDto>> getMyWrittenBoards(@AuthenticationPrincipal Member member) {
        List<MyWrittenResDto> result = myBoardService.getMyBoards(member.getId());
        return ResponseEntity.ok(result);
    }

    // 댓글 단 글
    @GetMapping("/commented")
    public ResponseEntity<List<MyWrittenResDto>> getCommentedBoards(@AuthenticationPrincipal Member member) {
        List<MyWrittenResDto> result = myBoardService.getBoardsICommented(member.getId());
        return ResponseEntity.ok(result);
    }
}
