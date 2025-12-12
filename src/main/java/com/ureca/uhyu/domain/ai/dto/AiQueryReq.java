package com.ureca.uhyu.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AiQueryReq(
        @Schema(description = "사용자 자연어 입력", example = "잠실 근처 조용한 카페")
        String userText,

        @Schema(description = "기본 반경 (미터)", example = "1000", defaultValue = "1000")
        Integer defaultRadius
) {}
