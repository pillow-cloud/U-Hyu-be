package com.ureca.uhyu.domain.brand.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "제휴처 목록 조회 응답")
public record BrandListRes(
        @Schema(description = "제휴처 리스트")
        List<BrandRes> brandList,
        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,
        @Schema(description = "전체 페이지 수")
        int totalPages,
        @Schema(description = "현재 페이지 번호")
        int currentPage
) {
    public static BrandListRes from(List<BrandRes> list, boolean hasNext, int totalPages, int currentPage) {
        return new BrandListRes(list, hasNext, totalPages, currentPage);
    }
}
