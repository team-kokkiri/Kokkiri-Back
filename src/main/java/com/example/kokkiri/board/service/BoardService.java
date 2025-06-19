package com.example.kokkiri.board.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardType;
import com.example.kokkiri.board.dto.*;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.board.repository.BoardTypeRepository;
import com.example.kokkiri.comment.dto.CommentListResDto;
import com.example.kokkiri.member.domain.Member;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardTypeRepository boardTypeRepository;

    // 게시글 작성
    public Board createBoard(BoardCreateReqDto boardCreateReqDto, Member member) {
        BoardType boardType = boardTypeRepository.findById(boardCreateReqDto.getBoardTypeId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시판 타입입니다."));

        Board board = Board.builder()
                .member(member)
                .boardType(boardType)
                .boardTitle(boardCreateReqDto.getBoardTitle())
                .boardContent(boardCreateReqDto.getBoardContent())
                .build();

        return boardRepository.save(board);
    }

    // 자유게시판
    public List<Board> findBoardList(Long boardId) {
        return boardRepository.findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(boardId, "N");
    }

    // BEST 게시판
    public List<Board> findPopularBoards(Long boardId) {
        return boardRepository.findByBoardTypeIdAndDelYnOrderByLikeCountDescCreatedTimeDesc(boardId, "N");
    }

    // 게시글 상세조회
    public BoardDetailResDto findBoardDetail(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        List<CommentListResDto> boardComments = board.getBoardComments().stream()
                .map(comment -> new CommentListResDto(
                        comment.getId(),
                        comment.getMember().getId(),
//                        comment.getParent().getId(),
                        comment.getCommentContent(),
                        comment.getCreatedTime()
                ))
                .collect(Collectors.toList());

        return BoardDetailResDto.builder()
                .id(board.getId())
                .boardTitle(board.getBoardTitle())
                .boardContent(board.getBoardContent())
                .writer(board.getMember().getNickname())
                .likeCount(board.getLikeCount())
                .commentCount(board.getBoardComments().size())
                .boardCreatedAt(board.getCreatedTime())
                .comments(boardComments)
                .build();
    }

    // 게시글 수정
    public void updateBoard(Long boardId, Member member, BoardUpdateReqDto boardUpdateReqDto) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        // 권한 체크
        if (!board.getMember().getId().equals(member.getId())) {
            System.out.println("게시글 수정 권한 없음 예외 발생");
            throw new AccessDeniedException("게시글 수정 권한이 없습니다.");
        }
        board.update(boardUpdateReqDto.getBoardTitle(), boardUpdateReqDto.getBoardContent());
    }

    // 게시글 삭제
    public void softDeleteBoard(Long boardId, Member member) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        // 권한 체크
        if (!board.getMember().getId().equals(member.getId())) {
            System.out.println("게시글 삭제 권한 없음 예외 발생");
            throw new AccessDeniedException("게시글 삭제 권한이 없습니다.");
        }
        boardRepository.delete(board);
    }

    // 페이징 처리
    public BoardPageResDto getBoardPage(Long typeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Board> boardPage = (typeId == 3L)
                ? boardRepository.findByBoardTypeIdAndDelYnOrderByLikeCountDescCreatedTimeDesc(typeId, "N", pageable)
                : boardRepository.findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(typeId, "N", pageable);

        List<BoardListResDto> boardListResDtos = boardPage.getContent().stream()
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

        return new BoardPageResDto(
                boardListResDtos,
                boardPage.getNumber(),
                boardPage.getTotalPages(),
                boardPage.getTotalElements(),
                boardPage.isLast()
        );
    }
}
