package com.example.kokkiri.comment.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.comment.domain.Comment;
import com.example.kokkiri.comment.domain.CommentLike;
import com.example.kokkiri.comment.dto.CommentCreateReqDto;
import com.example.kokkiri.comment.dto.CommentUpdateReqDto;
import com.example.kokkiri.comment.repository.CommentLikeRepository;
import com.example.kokkiri.comment.repository.CommentRepository;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.notification.domain.NotificationType;
import com.example.kokkiri.notification.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
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
    private final CommentLikeRepository commentLikeRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    // 댓글 작성
    public Comment createComment(Long boardId, Member commenter, CommentCreateReqDto commentCreateReqDto) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        // parentId가 있으면 대댓글
        Comment parent = null;
        if (commentCreateReqDto.getParentId() != null) {
            parent = commentRepository.findById(commentCreateReqDto.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("부모 댓글이 없습니다."));
        }

        Comment comment = Comment.builder()
                .board(board)
                .member(commenter)
                .commentContent(commentCreateReqDto.getComment())
                .parent(parent)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // 게시글 작성자에게 알림 보내기
        Member postWriter = board.getMember();
        if (!postWriter.getId().equals(commenter.getId())) {
            String content = commenter.getNickname() + "님이 회원님의 게시글에 댓글을 남겼습니다.";
            notificationService.send(postWriter, NotificationType.COMMENT, content, String.valueOf(board.getId()), null, savedComment.getCreatedTime());
        }

        // 댓글 작성자에게 알림 보내기
        if (parent != null && !parent.getMember().getId().equals(commenter.getId())) {
            String content = commenter.getNickname() + "님이 회원님의 댓글에 답글을 남겼습니다.";
            notificationService.send(parent.getMember(), NotificationType.REPLY, content, String.valueOf(boardId), null, savedComment.getCreatedTime());
        }

        return savedComment;
    }

    // 댓글 수정
    public void updateComment(Long commentId, Member member, CommentUpdateReqDto commentUpdateReqDto) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));
        if (!comment.getMember().getId().equals(member.getId())) {
            throw new AccessDeniedException("댓글 수정 권한이 없습니다.");
        }
        comment.update(commentUpdateReqDto.getComment());
    }

    // 댓글 삭제
    public void softDeleteComment(Long commentId, Member member) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));
        if (!comment.getMember().getId().equals(member.getId())) {
            throw new AccessDeniedException("댓글 삭제 권한이 없습니다.");
        }
        comment.markDeleted();
        commentRepository.save(comment);
    }

    // 댓글 좋아요
    public void likeComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글이 존재하지 않습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보가 존재하지 않습니다."));

        // 이미 좋아요 했는지 확인
        boolean alreadyLiked = commentLikeRepository.existsByCommentAndMember(comment, member);
        if (alreadyLiked) {
            throw new IllegalStateException("이미 좋아요를 누르셨습니다.");
        }

        CommentLike commentLike = CommentLike.builder()
                .comment(comment)
                .member(member)
                .build();

        commentLikeRepository.save(commentLike);

        comment.increaseLikeCount();
    }

}
