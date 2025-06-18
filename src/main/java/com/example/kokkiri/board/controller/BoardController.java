package com.example.kokkiri.board.controller;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.dto.*;
import com.example.kokkiri.board.service.BoardService;
import com.example.kokkiri.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    // 게시글 작성
    @PostMapping
    public ResponseEntity<?> createBoard(@AuthenticationPrincipal Member member,
                                         @RequestBody BoardCreateReqDto boardCreateReqDto) {
        Board board = boardService.createBoard(boardCreateReqDto, member);
        return ResponseEntity.ok(board.getId());
    }

    // 자유게시판 리스트조회
    @GetMapping("/list/{typeId}")
    public ResponseEntity<List<BoardListResDto>> getBoardList(@AuthenticationPrincipal Member member,
                                                              @PathVariable Long typeId) {
        // 로그인 필요 게시판
        if ((typeId == 2L || typeId == 3L || typeId == 4L) && member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); // 401
        }

        List<Board> boards;
        if (typeId == 3L) boards = boardService.findPopularBoards(typeId); // BEST 게시판
        else boards = boardService.findBoardList(typeId); // 자유, 공지사항, 자료공유 게시판

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
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBoard(@PathVariable Long id,
                                         @AuthenticationPrincipal Member member,
                                         @RequestBody BoardUpdateReqDto boardUpdateReqDto) {
        boardService.updateBoard(id, member, boardUpdateReqDto);
        return ResponseEntity.ok(boardUpdateReqDto);
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBoard(@AuthenticationPrincipal Member member,
                                         @PathVariable Long id) {
        boardService.softDeleteBoard(id, member);
        return ResponseEntity.ok(id);
    }

    // 댓글 작성
    @PostMapping("/detail/{id}/comments")
    public ResponseEntity<?> createComment(@PathVariable Long id, @RequestBody CommentCreateReqDto commentCreateReqDto) {
        boardService.createComment(id, commentCreateReqDto);
        return ResponseEntity.ok().build();
    }

}
