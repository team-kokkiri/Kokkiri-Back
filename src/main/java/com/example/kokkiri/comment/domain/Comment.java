package com.example.kokkiri.comment.domain;

import com.example.kokkiri.board.domain.Board;
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
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent; // 대댓글(부모 댓글)

    @Column(nullable = false)
    private String commentContent;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    private int reportCount = 0;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String delYn = "N";

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @OrderBy("createdTime ASC")
    private List<Comment> replies = new ArrayList<>();

    // 댓글 수정
    public void update(String newContent) {
        this.commentContent = newContent;
    }

    // 댓글 삭제
    public void markDeleted() {
        this.delYn = "Y";
    }

    // 댓글 좋아요
    public void increaseLikeCount() {
        this.likeCount += 1;
    }

    // 신고 카운트 증가
    public void increaseReportCount() {
        this.reportCount++;
    }
}
