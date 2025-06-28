package com.example.kokkiri.comment.repository;

import com.example.kokkiri.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
                SELECT COUNT(c) FROM Comment c
                    WHERE c.board.id = :boardId
                      AND c.delYn = 'N'
            """)
    Long countAllNotDeletedByBoardId(@Param("boardId") Long boardId);

    List<Comment> findByBoardId(Long boardId);
}
