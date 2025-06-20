package com.example.kokkiri.common.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardFile;
import com.example.kokkiri.common.config.FileUploadConfig;
import com.example.kokkiri.common.util.FileUtils;
import com.example.kokkiri.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileUploadConfig fileUploadConfig;

    /**
     * 업로드된 파일을 서버 디스크에 저장하고, 저장된 파일의 정보를 기반으로 BoardFile 엔티티를 생성
     *
     * @param file   업로드된 MultipartFile
     * @param member 파일을 업로드한 사용자
     * @param board  해당 파일이 속한 게시글
     * @return BoardFile 엔티티 (DB 저장용 객체)
     */
    public BoardFile saveFile(MultipartFile file, Member member, Board board) {
        // 고유한 파일명 생성 (UUID + 확장자)
        String savedName = FileUtils.generateFileName(file.getOriginalFilename());
        String filePath = FileUtils.saveFile(file, fileUploadConfig.getUploadDir(), savedName);

        return BoardFile.builder()
                .originalName(file.getOriginalFilename())
                .savedName(savedName)
                .filePath(filePath)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .member(member)
                .board(board)
                .build();
    }

    /**
     * 서버 디스크에서 파일을 삭제
     *
     * @param filePath 삭제할 파일의 전체 경로 (절대 경로)
     */
    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + e.getMessage(), e);
        }
    }

}
