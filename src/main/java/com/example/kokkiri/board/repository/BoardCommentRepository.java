package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

}
