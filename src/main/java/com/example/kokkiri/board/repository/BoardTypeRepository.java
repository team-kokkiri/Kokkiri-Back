package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardTypeRepository extends JpaRepository<BoardType, Long> {
}
