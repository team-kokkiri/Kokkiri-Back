package com.example.kokkiri.member.repository;

import com.example.kokkiri.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByNickname(String nickname);
    @Query("""
    SELECT m FROM Member m
    WHERE (LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
       OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:lastId IS NULL OR m.id < :lastId)
    ORDER BY m.id DESC
    """)
    List<Member> searchByKeyword(
            @Param("keyword") String keyword,
            @Param("lastId") Long lastId,
            Pageable pageable
    );
}
