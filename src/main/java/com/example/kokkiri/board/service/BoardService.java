package com.example.kokkiri.board.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardFile;
import com.example.kokkiri.board.domain.BoardLike;
import com.example.kokkiri.board.domain.BoardType;
import com.example.kokkiri.board.dto.*;
import com.example.kokkiri.board.repository.BoardFileRepository;
import com.example.kokkiri.board.repository.BoardLikeRepository;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.board.repository.BoardTypeRepository;
import com.example.kokkiri.comment.dto.CommentListResDto;
import com.example.kokkiri.comment.repository.CommentRepository;
import com.example.kokkiri.common.service.FileService;
import com.example.kokkiri.member.domain.Member;
import com.example.kokkiri.member.repository.MemberRepository;
import com.example.kokkiri.notification.domain.NotificationType;
import com.example.kokkiri.notification.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardTypeRepository boardTypeRepository;
    private final BoardFileRepository boardFileRepository;
    private final FileService fileService;
    private final MemberRepository memberRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    // 게시글 작성

    /**
     * 게시글을 생성하고, 해당 게시글에 첨부된 파일을 저장합니다.
     *
     * @param boardCreateReqDto 게시글 제목, 내용, 게시판 타입 ID 등이 담긴 DTO
     * @param member            작성자 (인증된 사용자 정보)
     * @param files             첨부파일 리스트 (없을 수도 있음)
     * @return 생성된 게시글 엔티티
     */
    public Board createBoard(BoardCreateReqDto boardCreateReqDto, Member member, List<MultipartFile> files) {
        BoardType boardType = boardTypeRepository.findById(boardCreateReqDto.getBoardTypeId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 게시판 타입입니다."));

        Board board = Board.builder()
                .member(member)
                .boardType(boardType)
                .boardTitle(boardCreateReqDto.getBoardTitle())
                .boardContent(boardCreateReqDto.getBoardContent())
                .questionYn(boardCreateReqDto.getQuestionYn())
                .build();

        boardRepository.save(board);

        // 첨부파일 있을때
        if (files != null) {
            for (MultipartFile file : files) {
                BoardFile boardFile = fileService.saveFile(file, member, board);
                boardFileRepository.save(boardFile);
            }
        }
        return board;
    }

    // 게시글 조회
    // 자유게시판
    public List<BoardListResDto> getBoardListMerged(Long typeId) {
        // 질문글
        List<Board> questionBoards = boardRepository.findUnansweredQuestionBoards(typeId);
        // 일반글
        List<Board> normalBoards = boardRepository.findByBoardTypeIdAndDelYnAndQuestionYnFalseOrderByCreatedTimeDesc(typeId, "N");

        // 질문글 → DTO 변환
        List<BoardListResDto> pinnedQuestionDtos = questionBoards.stream().map(this::boardListResDto).toList();
        // 일반글 → DTO 변환
        List<BoardListResDto> normalBoardDtos = normalBoards.stream().map(this::boardListResDto).toList();

        // 질문글 → 일반글 순으로 합치기
        List<BoardListResDto> merged = new ArrayList<>();
        merged.addAll(pinnedQuestionDtos);
        merged.addAll(normalBoardDtos);

        return merged;
    }

    // BEST 게시판
    public List<BoardListResDto> getBestBoardsFromFreeBoard() {
        List<Board> boards = boardRepository.findBestBoards();
        return boards.stream().map(this::boardListResDto).toList();
    }

    // 사이드 게시글 프리뷰
    public List<BoardListResDto> getPreview(Long typeId, int size) {
        Pageable pageable = PageRequest.of(0, size);
        Page<Board> boardPage = boardRepository.findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(typeId, "N", pageable);
        return boardPage.getContent().stream().map(this::boardListResDto).toList();
    }

    // 게시글 상세조회
    public BoardDetailResDto getBoardDetail(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        // 댓글 리스트
        List<CommentListResDto> boardComments = board.getBoardComments().stream()
                // 삭제된 댓글이면서 답글이 없음
                .filter(comment -> !(comment.getDelYn().equals("Y") && comment.getReplies().isEmpty()))
                .map(comment -> new CommentListResDto(
                        comment.getId(),
                        comment.getMember().getId(),
                        comment.getMember().getNickname(),
                        comment.getParent() != null ? comment.getParent().getId() : null,
                        comment.getDelYn().equals("Y") ? "(삭제된 댓글입니다.)" : comment.getCommentContent(),
                        comment.getLikeCount(),
                        comment.getDelYn().equals("Y"),
                        comment.getCreatedTime()
                ))
                .collect(Collectors.toList());

        // 파일 URL 생성
        List<String> fileUrls = board.getBoardFiles().stream()
                .map(file -> "/api/files/" + file.getSavedName())
                .toList();

        return BoardDetailResDto.builder()
                .id(board.getId())
                .boardTitle(board.getBoardTitle())
                .boardContent(board.getBoardContent())
                .writer(board.getMember().getNickname())
                .likeCount(board.getLikeCount())
                .commentCount(board.getBoardComments().size())
                .boardCreatedAt(board.getCreatedTime())
                .comments(boardComments)
                .fileUrls(fileUrls)
                .build();
    }

    // 게시글 수정
    public void updateBoard(Long boardId, Member member, BoardUpdateReqDto boardUpdateReqDto, List<MultipartFile> newFiles) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        // 권한 체크
        if (!board.getMember().getId().equals(member.getId())) {
            System.out.println("게시글 수정 권한 없음 예외 발생");
            throw new AccessDeniedException("게시글 수정 권한이 없습니다.");
        }
        board.update(boardUpdateReqDto.getBoardTitle(), boardUpdateReqDto.getBoardContent());

        // 유지할 파일 ID 목록
        List<Long> keepFileIds = boardUpdateReqDto.getKeepFileIds() != null ? boardUpdateReqDto.getKeepFileIds() : List.of();

        // keepFileIds가 null인 경우 삭제 로직을 건너뜀
        if (keepFileIds != null) {
            List<BoardFile> filesToDelete = board.getBoardFiles().stream()
                    .filter(file -> !keepFileIds.contains(file.getId()))
                    .toList();

            for (BoardFile file : filesToDelete) {
                fileService.deleteFile(file.getFilePath()); // 디스크에서 삭제
                boardFileRepository.delete(file); // DB에서 삭제
            }
        }

        // 새로 추가된 파일 처리
        if (newFiles != null) {
            for (MultipartFile file : newFiles) {
                BoardFile savedFile = fileService.saveFile(file, member, board);
                boardFileRepository.save(savedFile);
            }
        }

    }

    // 게시글 삭제
    public void softDeleteBoard(Long boardId, Member member) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        // 권한 체크
        if (!board.getMember().getId().equals(member.getId())) {
            System.out.println("게시글 삭제 권한 없음 예외 발생");
            throw new AccessDeniedException("게시글 삭제 권한이 없습니다.");
        }
        boardRepository.delete(board);
    }

    // 게시글 좋아요
    public void likeBoard(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보가 존재하지 않습니다."));

        boolean alreadyLiked = boardLikeRepository.existsByBoardAndMember(board, member);
        if (alreadyLiked) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 좋아요 누름");
        }

        // 좋아요 저장
        BoardLike boardLike = BoardLike.builder()
                .board(board)
                .member(member)
                .build();
        board.increaseLikeCount();
        BoardLike saveBoardLike = boardLikeRepository.save(boardLike);

        // 게시글 작성자에게 알림 전송 (본인 글 좋아한 건 제외)
        Member postWriter = board.getMember();
        if (!postWriter.getId().equals(member.getId())) {
            String content = member.getNickname() + "님이 회원님의 게시글을 좋아합니다.";
            notificationService.send(
                    postWriter,
                    NotificationType.LIKE_BOARD,
                    content,
                    String.valueOf(board.getId()),
                    null,
                    saveBoardLike.getCreatedTime()
            );
        }
    }

    // 페이징 처리
    public BoardPageResDto getBoardPage(Long typeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Board> boardPage = (typeId == 3L)
                ? boardRepository.findBestBoardsPage(10, "N", pageable)
                : boardRepository.findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(typeId, "N", pageable);

        List<BoardListResDto> boardListResDtos = boardPage.getContent().stream().map(this::boardListResDto).toList();

        return new BoardPageResDto(
                boardListResDtos,
                boardPage.getNumber(),
                boardPage.getTotalPages(),
                boardPage.getTotalElements(),
                boardPage.isLast()
        );
    }

    // boardListResDto 메서드 추출
    private BoardListResDto boardListResDto(Board board) {
        Long commentCount = commentRepository.countAllByBoardId(board.getId());

        String thumbnailUrl = board.getBoardFiles().stream()
                // "image/"로 시작하는 타입만 필터링
                .filter(file -> file.getFileType() != null && file.getFileType().startsWith("image"))
                // 조건을 통과한 이미지 파일 중 첫 번째 파일
                .findFirst()
                // 첫 번째 이미지 파일이 있으면 그 객체에서 실제 저장된 파일 경로를 꺼냄 (썸네일 URL로 사용)
                .map(BoardFile::getFilePath)
                .orElse(null);

        return new BoardListResDto(
                board.getId(),
                board.getBoardTitle(),
                board.getBoardContent(),
                board.getMember().getNickname(),
                board.getLikeCount(),
                commentCount,
                board.getCreatedTime(),
                board.getBoardType().getTypeName(),
                board.getQuestionYn(),
                thumbnailUrl
        );
    }
}
