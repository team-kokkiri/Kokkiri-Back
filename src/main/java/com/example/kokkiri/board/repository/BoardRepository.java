package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 게시글 검색
    // List<Board> findByBoardTitleContaining(String keyword);

    // 삭제되지 않은 게시글만 조회 (delYn = 'N')
    // List<Board> findByDelYn(String delYn);

    // 게시판 타입별 게시글 조회
    // List<Board> findByBoardType(BoardType boardType);

    // 좋아요 수 기준으로 정렬
    // List<Board> findAllByOrderByLikeCountDesc();

}
