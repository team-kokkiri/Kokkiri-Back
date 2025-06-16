package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 삭제되지 않은 게시글만 조회
    List<Board> findByDelYnOrderByCreatedTimeDesc(String delYn);

    // 좋아요 수 기준으로 정렬
    List<Board> findByDelYnOrderByLikeCountDescCreatedTimeDesc(String delYn);

    // 게시판 타입별 게시글 조회
    // List<Board> findByBoardType(BoardType boardType);

}
