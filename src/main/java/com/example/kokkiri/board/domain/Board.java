package com.example.kokkiri.board.domain;

import com.example.kokkiri.comment.domain.Comment;
import com.example.kokkiri.common.domain.BaseTimeEntity;
import com.example.kokkiri.member.domain.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//@SQLDelete(sql = "UPDATE board SET del_yn = 'Y' WHERE id = ?")
//@Where(clause = "delYn = 'N'")
public class Board extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String boardTitle;

    @Column(nullable = false, length = 10000)
    private String boardContent;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String delYn = "N";

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    private Boolean questionYn = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_type_id", nullable = false)
    private BoardType boardType;

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE)
    private List<BoardLike> boardLikes = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("createdTime ASC")
    private List<Comment> boardComments = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE)
    private List<BoardFile> boardFiles = new ArrayList<>();

    // 게시글 수정
    public void update(String title, String content) {
        this.boardTitle = title;
        this.boardContent = content;
    }

    // 게시글 삭제
    public void markDeleted() {
        this.delYn = "Y";
    }

    // 게시글 좋아요
    public void increaseLikeCount() {
        this.likeCount += 1;
    }

    // 질문글에서 일반글으로 변경
    public void setQuestionYn(boolean questionYn) {
        this.questionYn = questionYn;
    }

}
