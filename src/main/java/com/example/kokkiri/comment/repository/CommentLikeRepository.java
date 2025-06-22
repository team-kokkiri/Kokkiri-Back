package com.example.kokkiri.comment.repository;

import com.example.kokkiri.comment.domain.Comment;
import com.example.kokkiri.comment.domain.CommentLike;
import com.example.kokkiri.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentAndMember(Comment comment, Member member);
}
