package com.example.kokkiri.comment.repository;

import com.example.kokkiri.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {


}
