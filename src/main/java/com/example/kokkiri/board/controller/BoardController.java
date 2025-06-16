package com.example.kokkiri.board.controller;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.dto.BoardCreateReqDto;
import com.example.kokkiri.board.dto.BoardListResDto;
import com.example.kokkiri.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    // 생성
    @PostMapping("/create")
    public ResponseEntity<?> createBoard(@RequestBody BoardCreateReqDto boardCreateReqDto) {
        Board board = boardService.createBoard(boardCreateReqDto);
        return new ResponseEntity<>(board.getId(), HttpStatus.OK);
    }

    // 조회 - 생성일순
    @GetMapping("/list")
    public ResponseEntity<List<BoardListResDto>> getBoardList() {
        List<Board> boards = boardService.findBoardList();

        List<BoardListResDto> boardListResDtos = boards.stream()
                .map(board -> new BoardListResDto(
                        board.getId(),
                        board.getBoardTitle(),
                        board.getBoardContent(),
                        board.getMember().getNickname(),
                        board.getLikeCount(),
//                        board.getBoardComments(),
                        board.getCreatedTime(),
                        board.getBoardType().getTypeName()
                ))
                .toList();

        return ResponseEntity.ok(boardListResDtos);
    }

    // 조회 - 좋아요순
    @GetMapping("/popular")
    public ResponseEntity<List<BoardListResDto>> getPopularBoards() {
        List<Board> boards = boardService.findPopularBoards();
        List<BoardListResDto> boardListResDtos = boards.stream()
                .map(board -> new BoardListResDto(
                        board.getId(),
                        board.getBoardTitle(),
                        board.getBoardContent(),
                        board.getMember().getNickname(),
                        board.getLikeCount(),
//                        board.getBoardComments(),
                        board.getCreatedTime(),
                        board.getBoardType().getTypeName()
                ))
                .toList();

        return ResponseEntity.ok(boardListResDtos);
    }

    // 수정
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateBoard(@PathVariable Long id, @RequestBody BoardCreateReqDto boardCreateReqDto) {
        boardService.updateBoard(id, boardCreateReqDto);
        return ResponseEntity.ok().build();
    }

    // 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long id) {
        boardService.softDeleteBoard(id);
        return ResponseEntity.ok().build();
    }


}
