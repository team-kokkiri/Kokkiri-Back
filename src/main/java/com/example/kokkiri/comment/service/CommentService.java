package com.example.kokkiri.comment.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.comment.domain.Comment;
import com.example.kokkiri.comment.dto.CommentCreateReqDto;
import com.example.kokkiri.comment.dto.CommentUpdateReqDto;
import com.example.kokkiri.comment.repository.CommentRepository;
import com.example.kokkiri.member.domain.Member;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {

    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    // 댓글 작성
    public Comment createComment(Long boardId, Member member, CommentCreateReqDto commentCreateReqDto) {
        Board board = boardRepository.findById(commentCreateReqDto.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .board(board)
                .member(member)
                .commentContent(commentCreateReqDto.getComment())
                .build();

        return commentRepository.save(comment);
    }

    // 댓글 수정
    public void updateComment(Long boardId, Long commentId, Member member, CommentUpdateReqDto commentUpdateReqDto) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getMember().getId().equals(member.getId())) {
            System.out.println("댓글 수정 권한 없음 예외 발생");
            throw new AccessDeniedException("댓글 수정 권한이 없습니다.");
        }
        comment.update(commentUpdateReqDto.getComment());
    }

    // 댓글 삭제
    public void softDeleteComment(Long commentId, Member member) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getMember().getId().equals(member.getId())) {
            System.out.println("댓글 삭제 권한 없음 예외 발생");
            throw new AccessDeniedException("댓글 삭제 권한이 없습니다.");
        }
        commentRepository.delete(comment);
    }

}
