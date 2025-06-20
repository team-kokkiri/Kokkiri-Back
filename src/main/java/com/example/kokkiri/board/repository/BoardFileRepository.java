package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.BoardFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardFileRepository extends JpaRepository<BoardFile, Long> {
}
