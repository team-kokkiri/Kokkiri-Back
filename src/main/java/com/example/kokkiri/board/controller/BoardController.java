package com.example.kokkiri.board.controller;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.dto.BoardCreateReqDto;
import com.example.kokkiri.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @PostMapping("/board")
    public ResponseEntity<Board> createBoard(@RequestBody BoardCreateReqDto boardCreateReqDto) {
        return ResponseEntity.ok(boardService.createBoard(boardCreateReqDto));
    }

    
}
