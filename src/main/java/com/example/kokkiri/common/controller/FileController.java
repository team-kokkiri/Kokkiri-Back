package com.example.kokkiri.common.controller;

import com.example.kokkiri.common.config.FileUploadConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    // 파일 업로드 경로 설정 (application.yml 에서 주입)
    private final FileUploadConfig fileUploadConfig;

    /**
     * 저장된 파일을 URL로 요청하면 반환해주는 핸들러
     * 예: GET /api/files/uuid형식.jpg
     */
    @GetMapping("/{filename:.+}") // .+ 정규식: .확장자 포함한 전체 파일명을 파라미터로 받음
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            // 업로드 디렉토리에서 해당 파일 경로를 생성
            Path filePath = Path.of(fileUploadConfig.getUploadDir()).resolve(filename).normalize();
            // 파일 경로로 리소스 객체 생성
            Resource resource = new UrlResource(filePath.toUri());

            // 파일이 존재하지 않거나 읽을 수 없을 경우 404 Not Found 반환
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // 파일의 MIME 타입 결정 (예: image/jpeg, application/pdf 등)
            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}