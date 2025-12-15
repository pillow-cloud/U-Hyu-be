package com.ureca.uhyu.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ureca.uhyu.domain.ai.dto.AiQueryReq;
import com.ureca.uhyu.domain.ai.dto.AiQueryRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    private final ObjectMapper objectMapper;
    private final com.ureca.uhyu.domain.brand.repository.BrandRepository brandRepository;
    private final com.ureca.uhyu.domain.brand.repository.CategoryRepository categoryRepository;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    private String systemPrompt;

    public AiQueryRes convertQueryToFilters(AiQueryReq req) {
        if (req.userText() == null || req.userText().trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        int defaultRadius = (req.defaultRadius() != null) ? req.defaultRadius() : 1000;

        try {
            if (systemPrompt == null) {
                systemPrompt = loadPrompt("prompts/search_intent_extraction.txt");
            }

            // 1. Intent Extraction (AI)
            com.ureca.uhyu.domain.ai.dto.AiIntentExtractionRes intent = extractIntent(req.userText());

            // 2. DB Grounding & Validation
            List<Long> brandIds = new java.util.ArrayList<>();
            List<Long> categoryIds = new java.util.ArrayList<>();
            List<String> unrecognizedFilters = new java.util.ArrayList<>();

            // Brands Process
            for (String brandKey : intent.brandKeywords()) {
               List<com.ureca.uhyu.domain.brand.entity.Brand> brands = brandRepository.findByBrandNameContaining(brandKey);
               if (brands.isEmpty()) {
                   unrecognizedFilters.add(brandKey);
               } else if (brands.size() == 1) {
                   brandIds.add(brands.get(0).getId());
               } else {
                   // Ambiguity Resolution: Exact match first, then shortest length
                   com.ureca.uhyu.domain.brand.entity.Brand bestMatch = brands.stream()
                           .filter(b -> b.getBrandName().equals(brandKey))
                           .findFirst()
                           .orElse(brands.stream()
                                   .min(java.util.Comparator.comparingInt(b -> b.getBrandName().length()))
                                   .orElse(brands.get(0)));
                   brandIds.add(bestMatch.getId());
               }
            }

            // Categories Process
            for (String catKey : intent.categoryKeywords()) {
                List<com.ureca.uhyu.domain.brand.entity.Category> categories = categoryRepository.findByCategoryNameContaining(catKey);
                if (categories.isEmpty()) {
                    unrecognizedFilters.add(catKey);
                } else if (categories.size() == 1) {
                    categoryIds.add(categories.get(0).getId());
                } else {
                    com.ureca.uhyu.domain.brand.entity.Category bestMatch = categories.stream()
                            .filter(c -> c.getCategoryName().equals(catKey))
                            .findFirst()
                            .orElse(categories.stream()
                                    .min(java.util.Comparator.comparingInt(c -> c.getCategoryName().length()))
                                    .orElse(categories.get(0)));
                    categoryIds.add(bestMatch.getId());
                }
            }

            // 3. Construct Final Response
            // Radius processing from criteria (optional enhancement, sticking to default for now unless parsed)
            // For V1, criteria are just passed as notes or potential future use. currently just logging/ignoring specific numeric parsing provided by AI in old logic.
            // But wait, old logic extracted radius. New prompt asks for 'criteria' strings.
            // Let's keep simple: use defaultRadius. If 'criteria' has 'near', maybe set small radius?
            // For safety and strict adherence to plan: just using extracted IDs.

            String notes = "Extracted Keywords: " + intent.brandKeywords() + ", " + intent.categoryKeywords() +
                    " / Unrecognized: " + unrecognizedFilters;

            return new AiQueryRes(defaultRadius, categoryIds, brandIds, unrecognizedFilters, notes, 1.0, false);

        } catch (Exception e) {
            log.error("AI Search Error: {}", e.getMessage(), e);
            // Fallback
            return AiQueryRes.fallback(defaultRadius);
        }
    }

    private com.ureca.uhyu.domain.ai.dto.AiIntentExtractionRes extractIntent(String userText) {
        Map<String, Object> requestBody = buildOpenAiRequest(userText, systemPrompt);

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(Duration.ofSeconds(5)) // Increased timeout slightly
                .block();

        return parseResponse(response);
    }

    private String loadPrompt(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private Map<String, Object> buildOpenAiRequest(String userText, String systemPrompt) {
        Map<String, Object> jsonSchema = Map.of(
                "name", "search_intent",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "brandKeywords", Map.of("type", "array", "items", Map.of("type", "string"), "description", "List of potential brand names"),
                                "categoryKeywords", Map.of("type", "array", "items", Map.of("type", "string"), "description", "List of potential category names"),
                                "criteria", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Other search criteria")
                        ),
                        "required", List.of("brandKeywords", "categoryKeywords", "criteria"),
                        "additionalProperties", false
                )
        );

        return Map.of(
                "model", openAiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userText)
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", jsonSchema
                ),
                "temperature", 0.0
        );
    }

    @SuppressWarnings("unchecked")
    private com.ureca.uhyu.domain.ai.dto.AiIntentExtractionRes parseResponse(Map<String, Object> response) {
        try {
            if (response == null || !response.containsKey("choices")) {
                throw new RuntimeException("Invalid response from OpenAI");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in OpenAI response");
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            String content = (String) message.get("content");

            JsonNode root = objectMapper.readTree(content);

            List<String> brands = new java.util.ArrayList<>();
            if (root.has("brandKeywords")) {
                root.get("brandKeywords").forEach(n -> brands.add(n.asText()));
            }

            List<String> categories = new java.util.ArrayList<>();
            if (root.has("categoryKeywords")) {
                root.get("categoryKeywords").forEach(n -> categories.add(n.asText()));
            }

            List<String> criteria = new java.util.ArrayList<>();
            if (root.has("criteria")) {
                root.get("criteria").forEach(n -> criteria.add(n.asText()));
            }

            return new com.ureca.uhyu.domain.ai.dto.AiIntentExtractionRes(brands, categories, criteria);

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response", e);
            throw new RuntimeException("Parsing failed", e);
        }
    }
}
