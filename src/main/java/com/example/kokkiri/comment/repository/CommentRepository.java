package com.example.kokkiri.comment.repository;

import com.example.kokkiri.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 전체 댓글 + 대댓글 수
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.board.id = :boardId")
    Long countAllByBoardId(@Param("boardId") Long boardId);

}
