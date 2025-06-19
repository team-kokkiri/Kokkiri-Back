package com.example.kokkiri.common.service;

import com.example.kokkiri.board.domain.Board;
import com.example.kokkiri.board.domain.BoardFile;
import com.example.kokkiri.common.config.FileUploadConfig;
import com.example.kokkiri.common.util.FileUtils;
import com.example.kokkiri.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileUploadConfig fileUploadConfig;

    // 업로드된 파일을 서버 디스크에 저장하고, 저장된 파일 정보를 기반으로 BoardFile 엔티티를 생성
    public BoardFile saveFile(MultipartFile file, Member member, Board board) {
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

}
