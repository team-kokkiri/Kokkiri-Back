//package com.example.kokkiri.notification.domain;
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Embeddable;
//import lombok.AccessLevel;
//import lombok.NoArgsConstructor;
//import lombok.Getter; // Getter 추가
//
//@Embeddable
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@Getter
//public class RelatedUrl {
//
//    // 연결될 URL을 저장할 필드
//    @Column(nullable = true, length = 2048) // URL이 필수가 아닐 수 있으므로 nullable = true
//    private String url;
//
//    public RelatedUrl(String url) {
//        // URL 유효성 검사 로직 추가 가능 (예: URL 형식 확인)
//        this.url = url; // null 또는 빈 문자열도 허용될 수 있음
//    }
//}
