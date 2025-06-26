package com.example.kokkiri.board.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.dto.MyWrittenResDto;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MyBoardService {

    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    // 내가 쓴 글
    public List<MyWrittenResDto> getMyBoards(Long memberId) {
        List<Board> boards = boardRepository.findBoardsByWriter(memberId);

        return boards.stream()
                .map(board -> {
                    Long commentCount = commentRepository.countAllByBoardId(board.getId());
                    return new MyWrittenResDto(board, commentCount);
                })
                .toList();
    }

    // 댓글 단글
    public List<MyWrittenResDto> getBoardsICommented(Long memberId) {
        List<Board> boards = boardRepository.findBoardsByMyComments(memberId);

        return boards.stream()
                .map(board -> {
                    Long commentCount = commentRepository.countAllByBoardId(board.getId());
                    return new MyWrittenResDto(board, commentCount);
                })
                .toList();
    }
}
