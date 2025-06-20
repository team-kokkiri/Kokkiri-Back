package com.example.kokkiri.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

public class FileUtils {

    /**
     * UUID 사용 이유:
     * 1. 파일 이름 중복 방지
     * - 동일한 이름(cat.jpg 등)의 파일 업로드 시 덮어쓰기 방지
     * 2. 보안 강화
     * - 사용자가 지정한 원래 파일명을 노출하지 않음
     * 3. 다중 사용자 환경에서의 충돌 방지
     * - 같은 시점에 동일한 이름의 파일을 올려도 충돌 없음
     *
     * @param originalFileName 업로드된 원본 파일명
     * @return 고유하게 변환된 저장용 파일명 (예: UUID.jpg)
     */
    public static String generateFileName(String originalFileName) {
        return UUID.randomUUID().toString() + originalFileName.substring(originalFileName.lastIndexOf("."));
    }

    /**
     * 지정된 경로에 MultipartFile을 저장 -> 저장된 파일의 절대 경로를 반환
     *
     * @param file      MultipartFile 객체 (업로드된 실제 파일)
     * @param uploadDir 파일 저장 경로 (디렉토리)
     * @param savedName 저장할 파일명 (UUID 기반 파일명)
     * @return 저장된 파일의 절대 경로
     */
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
