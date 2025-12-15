package com.ureca.uhyu.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureca.uhyu.domain.ai.dto.AiQueryReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiServiceTest {

    private final AiService aiService = new AiService(new ObjectMapper());

    @Test
    @DisplayName("검색어(userText)가 null이면 IllegalArgumentException이 발생한다")
    void throwExceptionWhenUserTextIsNull() {
        // given
        AiQueryReq req = new AiQueryReq(null, 1000);

        // when & then
        assertThatThrownBy(() -> aiService.convertQueryToFilters(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("검색어를 입력해주세요.");
    }

    @Test
    @DisplayName("검색어(userText)가 빈 문자열이면 IllegalArgumentException이 발생한다")
    void throwExceptionWhenUserTextIsEmpty() {
        // given
        AiQueryReq req = new AiQueryReq("   ", 1000);

        // when & then
        assertThatThrownBy(() -> aiService.convertQueryToFilters(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("검색어를 입력해주세요.");
    }
}
