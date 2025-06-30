package com.example.kokkiri.report.domain;

import lombok.Getter;

@Getter
public enum ReportReason {
    INAPPROPRIATE_CONTENT("게시판 성격에 부적절함"),
    ABUSIVE_LANGUAGE("욕설/비하"),
    INAPPROPRIATE_MEETING("음란물/불건전한 만남 및 대화"),
    COMMERCIAL_AD("상업적 광고 및 판매"),
    LEAK_OR_FRAUD("유출/사칭/사기"),
    TROLLING_OR_SPAM("낚시/놀람/도배"),
    POLITICAL_CONTENT("정당/정치인 비하 및 선거운동"),
    ILLEGAL_CONTENT("불법촬영물 등의 유통");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }
}
