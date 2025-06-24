package com.example.kokkiri.member.profile;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final String uploadDir = "uploads/profile"; // 프로젝트 기준 상대경로

    public LocalFileStorageService() {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
    }

    @Override
    public String store(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String newFilename = UUID.randomUUID().toString() + extension;

            File destination = new File(uploadDir + "/" + newFilename);
            file.transferTo(destination);

            // URL 반환 (서버 도메인 + 저장경로) — 실제 배포시 도메인 주소 넣기
            return "/uploads/profile/" + newFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}