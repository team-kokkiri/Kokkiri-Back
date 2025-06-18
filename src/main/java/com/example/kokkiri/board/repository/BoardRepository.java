package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 자유게시판
    List<Board> findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(Long BoardTypeId, String delYn);

    // BEST 게시판
    List<Board> findByBoardTypeIdAndDelYnOrderByLikeCountDescCreatedTimeDesc(Long BoardTypeId, String delYn);

    // 게시판 타입별 게시글 조회
    List<Board> findByBoardType(BoardType boardType);

}
