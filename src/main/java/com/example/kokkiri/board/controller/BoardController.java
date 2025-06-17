package com.example.kokkiri.board.controller;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.dto.*;
import com.example.kokkiri.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    // 게시글 작성
    @PostMapping
    public ResponseEntity<?> createBoard(@RequestBody BoardCreateReqDto boardCreateReqDto) {
        Board board = boardService.createBoard(boardCreateReqDto);
        return ResponseEntity.ok(board.getId());
    }

    //
    // 자유게시판 리스트조회
    @GetMapping("/list/{typeId}")
    public ResponseEntity<List<BoardListResDto>> getBoardList(@PathVariable Long typeId) {

        switch (typeId.intValue()) {
            // 자유게시판, 공지사항, 자료공유
            case 1, 2, 4 -> {
                List<Board> boards = boardService.findBoardList(typeId);
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
                        )).toList();
                return ResponseEntity.ok(boardListResDtos);
            }
            // BEST 게시판
            case 3 -> {
                List<Board> boards = boardService.findPopularBoards(typeId);
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
                        )).toList();
                return ResponseEntity.ok(boardListResDtos);
            }
            default -> {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    // 게시글 상세조회
    @GetMapping("/{id}")
    public ResponseEntity<BoardDetailResDto> getBoardDetail(@PathVariable Long id) {
        BoardDetailResDto boardDetailResDto = boardService.findBoardDetail(id);
        return ResponseEntity.ok(boardDetailResDto);
    }

    // 게시글 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBoard(@PathVariable Long id, @RequestBody BoardUpdateReqDto boardUpdateReqDto) {
        boardService.updateBoard(id, boardUpdateReqDto);
        return ResponseEntity.ok(boardUpdateReqDto);
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
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
