package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardLike;
import com.example.kokkiri.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {
    boolean existsByBoardAndMember(Board board, Member member);
}
