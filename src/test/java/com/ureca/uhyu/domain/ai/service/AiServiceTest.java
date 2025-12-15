package com.ureca.uhyu.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureca.uhyu.domain.ai.dto.AiQueryReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AiServiceTest {

    @org.mockito.Mock
    private com.ureca.uhyu.domain.brand.repository.BrandRepository brandRepository;

    @org.mockito.Mock
    private com.ureca.uhyu.domain.brand.repository.CategoryRepository categoryRepository;

    @org.mockito.Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @org.mockito.InjectMocks
    private AiService aiService;

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
