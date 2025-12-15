package com.ureca.uhyu.domain.ai.controller;

import com.ureca.uhyu.domain.ai.dto.AiQueryReq;
import com.ureca.uhyu.domain.ai.dto.AiQueryRes;
import com.ureca.uhyu.domain.ai.service.AiService;
import com.ureca.uhyu.global.response.CommonResponse;
import com.ureca.uhyu.global.response.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI", description = "AI 기반 검색 및 분석 API")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "자연어 검색 필터 변환", description = "사용자의 자연어 입력을 분석하여 지도 검색 필터로 변환합니다.")
    @PostMapping("/query-to-filters")
    public CommonResponse<AiQueryRes> convertQueryToFilters(@RequestBody AiQueryReq req) {
        return CommonResponse.success(ResultCode.SUCCESS, aiService.convertQueryToFilters(req));
    }
}
