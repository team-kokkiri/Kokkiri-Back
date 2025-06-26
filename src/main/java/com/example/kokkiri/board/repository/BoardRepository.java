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

    // 질문글
    @Query("""
            SELECT b FROM Board b
                WHERE b.boardType.id = :boardTypeId
                  AND b.delYn = 'N'
                  AND b.questionYn = true
                  AND SIZE(b.boardComments) = 0
            ORDER BY b.createdTime DESC
            """)
    List<Board> findUnansweredQuestionBoards(@Param("boardTypeId") Long boardTypeId);

    // BEST 게시판
    @Query("""
            SELECT b FROM Board b
                WHERE b.boardType.id = :typeId
                  AND b.delYn = 'N'
                  AND b.likeCount >= 10
            ORDER BY b.likeCount DESC, b.createdTime DESC
            """)
    List<Board> findBestBoards(@Param("typeId") Long typeId);

    // 자유게시판, 공지사항, 자료공유
    List<Board> findByBoardTypeIdAndDelYnAndQuestionYnFalseOrderByCreatedTimeDesc(Long BoardTypeId, String delYn);


    // 페이징 - 자유게시판, 공지사항, 자료공유
    Page<Board> findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(Long BoardTypeId, String delYn, Pageable pageable);

    // 페이징 - BEST 게시판
    Page<Board> findByBoardTypeIdAndDelYnOrderByLikeCountDescCreatedTimeDesc(Long boardTypeId, String delYn, Pageable pageable);

    @Query("""
            SELECT b FROM Board b
                WHERE b.boardType.id = :boardTypeId
                  AND b.delYn = :delYn
                  AND b.likeCount >= :likeCount
            ORDER BY b.likeCount DESC, b.createdTime DESC
            """)
    Page<Board> findBestBoardsPage(@Param("boardTypeId") Long boardTypeId,
                                   @Param("likeCount") int likeCount,
                                   @Param("delYn") String delYn,
                                   Pageable pageable);

}