package com.ureca.uhyu.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AiQueryRes(
        @Schema(description = "검색 반경 (미터)")
        int radius,

        @Schema(description = "카테고리 ID 목록 (DB 검증됨)")
        java.util.List<Long> categoryIds,

        @Schema(description = "브랜드 ID 목록 (DB 검증됨)")
        java.util.List<Long> brandIds,

        @Schema(description = "식별되지 않은 검색어 목록")
        java.util.List<String> unrecognizedFilters,

        @Schema(description = "AI 분석 코멘트")
        String notes,

        @Schema(description = "신뢰도 점수 (0.0 ~ 1.0)")
        double confidence,

        @Schema(description = "Fallback 적용 여부")
        boolean fallback
) {
        public static AiQueryRes fallback(int defaultRadius) {
                return new AiQueryRes(defaultRadius, java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList(), "기본 검색으로 진행합니다.", 0.0, true);
        }
}
