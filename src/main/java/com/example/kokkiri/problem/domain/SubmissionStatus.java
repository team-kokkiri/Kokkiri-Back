package com.example.kokkiri.problem.domain;

public enum SubmissionStatus {
    PENDING,                    // 채점 대기중
    ACCEPTED,                   // 정답
    WRONG_ANSWER,              // 오답
    RUNTIME_ERROR,             // 런타임 에러
    TIME_LIMIT_EXCEEDED,       // 시간 초과
    MEMORY_LIMIT_EXCEEDED,     // 메모리 초과
    COMPILE_ERROR              // 컴파일 에러
}
