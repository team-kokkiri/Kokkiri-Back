package com.example.kokkiri.board.controller;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.dto.BoardCreateReqDto;
import com.example.kokkiri.board.dto.BoardDetailResDto;
import com.example.kokkiri.board.dto.BoardListResDto;
import com.example.kokkiri.board.dto.BoardUpdateReqDto;
import com.example.kokkiri.board.service.BoardService;
import com.example.kokkiri.member.domain.Member;
import lombok.RequiredArgsConstructor;
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

    /**
     * @AuthenticationPrincipal: Authentication 객체 안에 들어있는 principal을 직접 꺼내주는 어노테이션
     * 현재 로그인한 사용자의 정보를 컨트롤러 메서드의 파라미터로 주입할 때 사용
     * 내부적으로 SecurityContextHolder.getContext().getAuthentication().getPrincipal()과 동일
     */
    @PostMapping
    public ResponseEntity<?> createBoard(@AuthenticationPrincipal Member member,
                                         @RequestBody BoardCreateReqDto boardCreateReqDto) {
        Board board = boardService.createBoard(boardCreateReqDto, member);
        return ResponseEntity.ok(board.getId());
    }

    // 게시글 리스트조회
    @GetMapping("/list/{typeId}")
    public ResponseEntity<List<BoardListResDto>> getBoardList(@PathVariable Long typeId) {

        List<Board> boards = (typeId == 3L)
                ? boardService.findPopularBoards(typeId) // BEST 게시판
                : boardService.findBoardList(typeId); // 자유, 공지사항, 자료공유 게시판

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
    @GetMapping("/detail/{boardId}")
    public ResponseEntity<BoardDetailResDto> getBoardDetail(@PathVariable Long boardId) {
        BoardDetailResDto boardDetailResDto = boardService.findBoardDetail(boardId);
        return ResponseEntity.ok(boardDetailResDto);
    }

    // 게시글 수정
    @PutMapping("/{boardId}")
    public ResponseEntity<?> updateBoard(@PathVariable Long boardId,
                                         @AuthenticationPrincipal Member member,
                                         @RequestBody BoardUpdateReqDto boardUpdateReqDto) {
        boardService.updateBoard(boardId, member, boardUpdateReqDto);
        return ResponseEntity.ok(boardUpdateReqDto);
    }

    // 게시글 삭제
    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long boardId,
                                         @AuthenticationPrincipal Member member) {
        boardService.softDeleteBoard(boardId, member);
        return ResponseEntity.ok(boardId);
    }

}
