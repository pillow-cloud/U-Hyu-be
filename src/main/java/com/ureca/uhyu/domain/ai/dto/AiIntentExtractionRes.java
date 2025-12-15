package com.ureca.uhyu.domain.ai.dto;

import java.util.List;

public record AiIntentExtractionRes(
        List<String> brandKeywords,
        List<String> categoryKeywords,
        List<String> criteria
) {
}
