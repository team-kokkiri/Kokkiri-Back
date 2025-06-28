package com.example.kokkiri.board.repository;

import com.example.kokkiri.board.domain.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    Page<Board> findByMemberIdAndDelYnOrderByCreatedTimeDesc(Long memberId, String delYn, Pageable pageable);


    /**
     * [내가 댓글 단 게시글 목록 조회 쿼리]
     * - 사용자가 작성한 댓글이 달린 게시글들을 중복 없이 조회
     * - 같은 게시글에 여러 댓글을 달았더라도 1개 게시글로만 표시
     * - 최신 댓글 순서로 정렬 (가장 최근에 댓글 단 게시글이 먼저 보임) / 안됨
     * - DISTINCT ON (PostgreSQL 문법): b.id 기준으로 게시글 하나만 추출
     * - nativeQuery 사용 이유: JPQL에서는 DISTINCT ON 지원하지 않음
     * - countQuery: 페이징 처리의 전체 게시글 개수를 정확하게 계산하기 위해 별도로 명시
     */
    @Query(value = """
            SELECT DISTINCT ON (b.id) b.*
            FROM comment c
            JOIN board b ON c.board_id = b.id
            WHERE c.member_id = :memberId
            ORDER BY b.id, c.created_time DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT b.id)
                    FROM comment c
                    JOIN board b ON c.board_id = b.id
                    WHERE c.member_id = :memberId
                    """,
            nativeQuery = true)
    Page<Board> findBoardsByMyComments(@Param("memberId") Long memberId, Pageable pageable);
    
    // 특정 날짜 이후 작성된 게시글 수 조회 (오늘 작성된 게시글)
    @Query("SELECT COUNT(b) FROM Board b WHERE b.createdTime >= :startDate AND b.delYn = 'N'")
    long countByCreatedTimeAfter(@Param("startDate") LocalDateTime startDate);

}