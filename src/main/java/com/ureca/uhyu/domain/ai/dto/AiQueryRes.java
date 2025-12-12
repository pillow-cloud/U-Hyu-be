package com.ureca.uhyu.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AiQueryRes(
        @Schema(description = "검색 반경 (미터)")
        int radius,

        @Schema(description = "카테고리 필터")
        String category,

        @Schema(description = "브랜드 필터")
        String brand,

        @Schema(description = "AI 분석 코멘트")
        String notes,

        @Schema(description = "신뢰도 점수 (0.0 ~ 1.0)")
        double confidence,

        @Schema(description = "Fallback 적용 여부")
        boolean fallback
) {
        public static AiQueryRes fallback(int defaultRadius) {
                return new AiQueryRes(defaultRadius, null, null, "기본 검색으로 진행합니다.", 0.0, true);
        }
}
