package com.example.kokkiri.board.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardType;
import com.example.kokkiri.board.dto.BoardCreateReqDto;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.board.repository.BoardTypeRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    // private final BoardCommentRepository boardCommentRepository;
    private final MemberRepository memberRepository;
    private final BoardTypeRepository boardTypeRepository;


    // 생성
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

    // 조회 - 생성일순
    public List<Board> findBoardList() {
        return boardRepository.findByDelYnOrderByCreatedTimeDesc("N");
    }

    // 조회 - 좋아요순
    public List<Board> findPopularBoards() {
        return boardRepository.findByDelYnOrderByLikeCountDescCreatedTimeDesc("N");
    }

    // 수정
    public void updateBoard(Long id, BoardCreateReqDto boardCreateReqDto) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("board is not found"));
        board.update(boardCreateReqDto.getBoardTitle(), boardCreateReqDto.getBoardContent());
    }

    // 삭제
    public void softDeleteBoard(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("board is not found"));
        boardRepository.delete(board);
//        board.setDelYn("Y");
    }

}
