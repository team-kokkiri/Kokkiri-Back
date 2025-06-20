package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 자유게시판, 공지사항, 자료공유
    List<Board> findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(Long BoardTypeId, String delYn);

    // BEST 게시판
    List<Board> findByBoardTypeIdAndDelYnOrderByLikeCountDescCreatedTimeDesc(Long BoardTypeId, String delYn);

    // 페이징 - 자유게시판, 공지사항, 자료공유
    Page<Board> findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(Long BoardTypeId, String delYn, Pageable pageable);

    // 페이징 - BEST 게시판
    Page<Board> findByBoardTypeIdAndDelYnOrderByLikeCountDescCreatedTimeDesc(Long boardTypeId, String delYn, Pageable pageable);
}
