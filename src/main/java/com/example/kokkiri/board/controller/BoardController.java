package com.example.kokkiri.board.controller;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.dto.*;
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

    // 게시글 작성
    @PostMapping("/create")
    public ResponseEntity<?> createBoard(@RequestBody BoardCreateReqDto boardCreateReqDto) {
        Board board = boardService.createBoard(boardCreateReqDto);
        return new ResponseEntity<>(board.getId(), HttpStatus.OK);
    }

    // 게시글 리스트조회 - 생성일순
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
                        board.getBoardComments().size(),
                        board.getCreatedTime(),
                        board.getBoardType().getTypeName()
                ))
                .toList();

        return ResponseEntity.ok(boardListResDtos);
    }

    // 게시글 리스트조회 - 좋아요순
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
                        board.getBoardComments().size(),
                        board.getCreatedTime(),
                        board.getBoardType().getTypeName()
                ))
                .toList();

        return ResponseEntity.ok(boardListResDtos);
    }

    // 게시글 상세조회
    @GetMapping("/detail/{id}")
    public ResponseEntity<BoardDetailResDto> getBoardDetail(@PathVariable Long id) {
        BoardDetailResDto boardDetailResDto = boardService.findBoardDetail(id);
        return ResponseEntity.ok(boardDetailResDto);
    }

    // 게시글 수정
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateBoard(@PathVariable Long id, @RequestBody BoardUpdateReqDto boardUpdateReqDto) {
        boardService.updateBoard(id, boardUpdateReqDto);
        return ResponseEntity.ok(boardUpdateReqDto);
    }

    // 게시글 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long id) {
        boardService.softDeleteBoard(id);
        return ResponseEntity.ok(id);
    }

    // 댓글 작성
    @PostMapping("/detail/{id}/comments")
    public ResponseEntity<?> createComment(@PathVariable Long id, @RequestBody CommentCreateReqDto commentCreateReqDto) {
        boardService.createComment(id, commentCreateReqDto);
        return ResponseEntity.ok().build();
    }

}
