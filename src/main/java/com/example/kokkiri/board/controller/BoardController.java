package com.example.kokkiri.board.controller;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardFile;
import com.example.kokkiri.board.dto.*;
import com.example.kokkiri.board.service.BoardService;
import com.example.kokkiri.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    /**
     * @AuthenticationPrincipal: Authentication 객체 안에 들어있는 principal을 직접 꺼내주는 어노테이션
     * 현재 로그인한 사용자의 정보를 컨트롤러 메서드의 파라미터로 주입할 때 사용
     * 내부적으로 SecurityContextHolder.getContext().getAuthentication().getPrincipal()과 동일
     * =============
     * @RequestBody : application/json   | JSON만 허용
     * @RequestPart: multipart/form-data | JSON + 파일
     */
    // 게시글 작성
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBoard(@AuthenticationPrincipal Member member,
                                         @RequestPart("board") BoardCreateReqDto boardCreateReqDto,
                                         @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        Board board = boardService.createBoard(boardCreateReqDto, member, files);
        return ResponseEntity.ok(board.getId());
    }

    // 게시글 리스트조회
    @GetMapping("/list/{typeId}")
    public ResponseEntity<List<BoardListResDto>> getBoardList(@PathVariable Long typeId) {

        List<Board> boards = (typeId == 3L)
                ? boardService.findPopularBoards(typeId) // BEST 게시판
                : boardService.findBoardList(typeId); // 자유, 공지사항, 자료공유 게시판

        List<BoardListResDto> boardListResDtos = boards.stream()
                .map(board -> {
                    String thumbnailUrl = board.getBoardFiles().stream()
                            // "image/"로 시작하는 타입만 필터링
                            .filter(file -> file.getFileType() != null && file.getFileType().startsWith("image"))
                            // 조건을 통과한 이미지 파일 중 첫 번째 파일
                            .findFirst()
                            // 첫 번째 이미지 파일이 있으면 그 객체에서 실제 저장된 파일 경로를 꺼냄 (썸네일 URL로 사용)
                            .map(BoardFile::getFilePath)
                            .orElse(null);

                    return new BoardListResDto(
                            board.getId(),
                            board.getBoardTitle(),
                            board.getBoardContent(),
                            board.getMember().getNickname(),
                            board.getLikeCount(),
                            board.getBoardComments().size(),
                            board.getCreatedTime(),
                            board.getBoardType().getTypeName(),
                            thumbnailUrl
                    );
                })
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
    @PutMapping(value = "/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBoard(@PathVariable Long boardId,
                                         @AuthenticationPrincipal Member member,
                                         @RequestPart("board") BoardUpdateReqDto boardUpdateReqDto,
                                         @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        boardService.updateBoard(boardId, member, boardUpdateReqDto, files);
        return ResponseEntity.ok().build();
    }

    // 게시글 삭제
    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(@PathVariable Long boardId,
                                         @AuthenticationPrincipal Member member) {
        boardService.softDeleteBoard(boardId, member);
        return ResponseEntity.ok(boardId);
    }

    // 페이징 게시글 리스트 조회
    @GetMapping("/list/{typeId}/{page}")
    public ResponseEntity<BoardPageResDto> getBoardPage(@PathVariable Long typeId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        BoardPageResDto boardPage = boardService.getBoardPage(typeId, page, size);
        return ResponseEntity.ok(boardPage);
    }


}
