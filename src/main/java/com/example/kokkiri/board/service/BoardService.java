package com.example.kokkiri.board.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.dto.BoardCreateReqDto;
import com.example.kokkiri.board.repository.BoardCommentRepository;
import com.example.kokkiri.board.repository.BoardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCommentRepository boardCommentRepository;

    // 생성
    public Board createBoard(BoardCreateReqDto boardCreateReqDto) {
        Board board = Board.builder()
                .memberId(boardCreateReqDto.getMemberId())
                .typeId(boardCreateReqDto.getTypeId())
                .boardTitle(boardCreateReqDto.getBoardTitle())
                .boardContent(boardCreateReqDto.getBoardContent())
                .delYn("N")
                .likeCount(0)
                .build();
        return boardRepository.save(board);
    }

    // 조회
    public


    // 수정

    // 삭제

}
