package com.example.kokkiri.member.profile;

import org.springframework.web.multipart.MultipartFile;

public interface  FileStorageService {
    String store(MultipartFile file);
}
