package com.example.kokkiri.board.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardFile;
import com.example.kokkiri.board.domain.BoardType;
import com.example.kokkiri.board.dto.*;
import com.example.kokkiri.board.repository.BoardFileRepository;
import com.example.kokkiri.board.repository.BoardRepository;
import com.example.kokkiri.board.repository.BoardTypeRepository;
import com.example.kokkiri.comment.dto.CommentListResDto;
import com.example.kokkiri.common.service.FileService;
import com.example.kokkiri.member.domain.Member;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    // 자유게시판
    public List<Board> findBoardList(Long boardId) {
        return boardRepository.findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(boardId, "N");
    }

    // BEST 게시판
    public List<Board> findPopularBoards(Long boardId) {
        return boardRepository.findByBoardTypeIdAndDelYnOrderByLikeCountDescCreatedTimeDesc(boardId, "N");
    }

    // 게시글 상세조회
    public BoardDetailResDto findBoardDetail(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        // 댓글 리스트
        List<CommentListResDto> boardComments = board.getBoardComments().stream()
                .map(comment -> new CommentListResDto(
                        comment.getId(),
                        comment.getMember().getId(),
//                        comment.getParent().getId(), // 답글
                        comment.getCommentContent(),
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

    // 페이징 처리
    public BoardPageResDto getBoardPage(Long typeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Board> boardPage = (typeId == 3L)
                ? boardRepository.findByBoardTypeIdAndDelYnOrderByLikeCountDescCreatedTimeDesc(typeId, "N", pageable)
                : boardRepository.findByBoardTypeIdAndDelYnOrderByCreatedTimeDesc(typeId, "N", pageable);

        List<BoardListResDto> boardListResDtos = boardPage.getContent().stream()
                .map(board -> new BoardListResDto(
                        board.getId(),
                        board.getBoardTitle(),
                        board.getBoardContent(),
                        board.getMember().getNickname(),
                        board.getLikeCount(),
                        board.getBoardComments().size(),
                        board.getCreatedTime(),
                        board.getBoardType().getTypeName(),
                        board.getBoardFiles() != null && !board.getBoardFiles().isEmpty()
                                ? board.getBoardFiles().get(0).getFilePath()
                                : null
                ))
                .toList();

        return new BoardPageResDto(
                boardListResDtos,
                boardPage.getNumber(),
                boardPage.getTotalPages(),
                boardPage.getTotalElements(),
                boardPage.isLast()
        );
    }
}
