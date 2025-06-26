package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 자유게시판, 공지사항, 자료공유
    List<Board> findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(Long BoardTypeId, String delYn);

    // BEST 게시판
    @Query("""
            SELECT b FROM Board b
                  WHERE b.delYn = 'N'
                    AND b.likeCount >= 10
            ORDER BY b.createdTime DESC
            """)
    List<Board> findBestBoards();

    // 페이징 - 자유게시판, 공지사항, 자료공유
    Page<Board> findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(Long BoardTypeId, String delYn, Pageable pageable);

    // 페이징 - BEST 게시판
    @Query("""
            SELECT b FROM Board b
                  WHERE b.delYn = 'N'
                    AND b.likeCount >= 10
            ORDER BY b.createdTime DESC
            """)
    Page<Board> findBestBoardsPage(Pageable pageable);

    // 내가 쓴 글
    @Query("""
            SELECT b FROM Board b
                WHERE b.member.id = :memberId
                  AND b.delYn = 'N'
            ORDER BY b.createdTime DESC
            """)
    List<Board> findBoardsByWriter(@Param("memberId") Long memberId);

    // 댓글 단 글
    @Query("""
            SELECT DISTINCT c.board FROM Comment c
                WHERE c.member.id = :memberId
                  AND c.board.delYn = 'N'
            ORDER BY c.board.createdTime DESC
            """)
    List<Board> findBoardsByMyComments(@Param("memberId") Long memberId);

    // 전체 게시판 검색
    @Query("""
            SELECT b FROM Board b
                WHERE b.delYn = 'N'
                  AND (
                       LOWER(b.boardTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                       LOWER(b.boardContent) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY b.createdTime DESC
            """)
    Page<Board> searchAllBoards(@Param("keyword") String keyword,
                                Pageable pageable);

    // 특정 게시판 검색
    @Query("""
            SELECT b FROM Board b
                WHERE b.delYn = 'N'
                  AND b.boardType.id = :typeId
                  AND (
                       LOWER(b.boardTitle) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                       LOWER(b.boardContent) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY b.createdTime DESC
            """)
    Page<Board> searchBoardsByType(@Param("typeId") Long typeId,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);


}