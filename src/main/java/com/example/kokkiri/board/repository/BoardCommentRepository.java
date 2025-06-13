package com.example.kokkiri.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCommentRepository extends JpaRepository {

    // 작성시간 기준 정렬
    // List<BoardComment> findAllOrderByCreatedTimeAsc();
}
