package com.example.kokkiri.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file")
@Getter
@Setter
/**
 * application.yml의 file.upload-dir 값을 가져오는 설정 클래스
 * */
public class FileUploadConfig {
    private String uploadDir;
}
