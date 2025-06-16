package com.example.kokkiri.board.domain;

import com.example.kokkiri.common.domain.BaseTimeEntity;
import com.example.kokkiri.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE board SET del_yn = 'Y' WHERE id = ?")
//@Where(clause = "delYn = 'N'")
public class Board extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String boardTitle;

    @Column(nullable = false)
    @Lob
    private String boardContent;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String delYn = "N";

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_type_id", nullable = false)
    private BoardType boardType;

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE)
    private List<BoardLike> boardLikes = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE)
    @OrderBy("createdTime ASC")
    private List<BoardComment> boardComments = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE)
    private List<BoardFile> boardFiles = new ArrayList<>();


    public void update(String title, String content) {
        this.boardTitle = title;
        this.boardContent = content;
    }

//    public void softDelete() {
//        this.delYn = "Y";
//    }

}
