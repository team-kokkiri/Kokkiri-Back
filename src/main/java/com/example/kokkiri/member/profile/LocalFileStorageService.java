package com.example.kokkiri.member.profile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload-dir:/uploads/profile}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file) {
        try {
            // 절대 경로로 업로드 디렉토리 생성
            Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "profile");
            
            // 디렉토리가 존재하지 않으면 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String newFilename = UUID.randomUUID().toString() + extension;

            Path destinationPath = uploadPath.resolve(newFilename);
            file.transferTo(destinationPath.toFile());

            // URL 반환 (서버 도메인 + 저장경로)
            return "/uploads/profile/" + newFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }
}