package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

}
