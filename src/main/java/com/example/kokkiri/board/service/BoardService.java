package com.example.kokkiri.board.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardComment;
import com.example.kokkiri.board.domain.BoardType;
import com.example.kokkiri.board.dto.*;
import com.example.kokkiri.board.repository.BoardCommentRepository;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.board.repository.BoardTypeRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final BoardTypeRepository boardTypeRepository;
    private final BoardCommentRepository boardCommentRepository;

    // 게시글 작성
    public Board createBoard(BoardCreateReqDto boardCreateReqDto) {

        Member member = memberRepository.findById(boardCreateReqDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("member is not found"));

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

    // 게시글 리스트조회 - 생성일순
    public List<Board> findBoardList() {
        return boardRepository.findByDelYnOrderByCreatedTimeDesc("N");
    }

    // 게시글 리스트조회 - 좋아요순
    public List<Board> findPopularBoards() {
        return boardRepository.findByDelYnOrderByLikeCountDescCreatedTimeDesc("N");
    }

    // 게시글 상세조회
    public BoardDetailResDto findBoardDetail(Long id) {
        Board board = boardRepository.findById(id)
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
    public void updateBoard(Long id, BoardUpdateReqDto boardUpdateReqDto) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        board.update(boardUpdateReqDto.getBoardTitle(), boardUpdateReqDto.getBoardContent());
    }

    // 게시글 삭제
    public void softDeleteBoard(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        boardRepository.delete(board);
//        board.setDelYn("Y");
    }

    // 댓글 작성
    public BoardComment createComment(Long id, CommentCreateReqDto commentCreateReqDto) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Member member = memberRepository.findById(commentCreateReqDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("member is not found"));

        BoardComment boardComment = BoardComment.builder()
                .board(board)
                .member(member)
                .commentContent(commentCreateReqDto.getComment())
                .build();

        return boardCommentRepository.save(boardComment);
    }

}
