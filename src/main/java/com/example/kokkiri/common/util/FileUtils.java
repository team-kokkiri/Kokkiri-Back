package com.example.kokkiri.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

public class FileUtils {

    /**
     * UUID 사용 이유
     * 1. 파일 이름 중복 방지
     * - 사용자가 cat.jpg, cat.pdf 같은 파일명을 여러 번 업로드하면, 기존 파일이 덮어쓰기 될 수 있음.
     * - UUID를 붙이면 f0ab3a34-b2cf-4a7c-b739-fcf7f1cc4322.jpg처럼 유일한 이름이 생성되어 덮어쓰기를 방지.
     * 2. 보안 강화
     * - 사용자가 업로드한 파일 이름을 그대로 저장하면 내부 경로가 노출될 수 있음.
     * - UUID로 변경 시 예측 불가능한 이름이 되므로, URL을 통한 무단 접근도 어려움.
     * 3. 파일 이름 충돌 방지 (다중 사용자 환경)
     * - 여러 사용자가 동시에 같은 이름의 파일을 업로드해도 충돌 없이 저장 가능.
     */
    // 고유한 파일 이름 생성
    public static String generateFileName(String originalFileName) {
        return UUID.randomUUID().toString() + originalFileName.substring(originalFileName.lastIndexOf("."));
    }

    // 파일 저장
    public static String saveFile(MultipartFile file, String uploadDir, String savedName) {
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs(); // 디렉토리 없으면 생성
            }
            File savedFile = new File(uploadDir, savedName);
            file.transferTo(savedFile); // 파일 저장

            return savedFile.getAbsolutePath(); // 저장 경로 반환
        } catch (Exception e) {
            throw new RuntimeException("파일 저장 실패: " + e.getMessage(), e);
        }
    }


//    public static String generateThumbnailFileName(String originalFileName) {
//        return "t_" + UUID.randomUUID().toString() + originalFileName.substring(originalFileName.lastIndexOf("."));
//    }
}
