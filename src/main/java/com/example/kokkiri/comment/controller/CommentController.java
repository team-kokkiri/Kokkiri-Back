package com.example.kokkiri.comment.controller;

import com.example.kokkiri.comment.dto.CommentCreateReqDto;
import com.example.kokkiri.comment.dto.CommentUpdateReqDto;
import com.example.kokkiri.comment.service.CommentService;
import com.example.kokkiri.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards/detail/{boardId}/comments")
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @PostMapping
    public ResponseEntity<?> createComment(@PathVariable("boardId") Long boardId,
                                           @AuthenticationPrincipal Member member,
                                           @RequestBody CommentCreateReqDto commentCreateReqDto) {
        commentService.createComment(boardId, member, commentCreateReqDto);
        return ResponseEntity.ok().build();
    }

    // 댓글 수정
    @PutMapping(value = "/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable("boardId") Long boardId,
                                           @PathVariable Long commentId,
                                           @AuthenticationPrincipal Member member,
                                           @RequestBody CommentUpdateReqDto commentUpdateReqDto) {
        commentService.updateComment(boardId, commentId, member, commentUpdateReqDto);
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping(value = "/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable("boardId") Long boardId,
                                           @PathVariable Long commentId,
                                           @AuthenticationPrincipal Member member) {
        commentService.softDeleteComment(commentId, member);
        return ResponseEntity.ok().build();
    }

}
