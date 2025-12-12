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
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .build();

    private String systemPrompt;

    public AiQueryRes convertQueryToFilters(AiQueryReq req) {
        int defaultRadius = (req.defaultRadius() != null) ? req.defaultRadius() : 1000;

        try {
            if (systemPrompt == null) {
                systemPrompt = loadPrompt("prompts/query_to_filters.txt");
            }

            Map<String, Object> requestBody = buildOpenAiRequest(req.userText(), systemPrompt);

            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(3))
                    .block();

            return parseResponse(response);

        } catch (Exception e) {
            log.error("AI Search Error: {}", e.getMessage(), e);
            // Fallback
            return AiQueryRes.fallback(defaultRadius);
        }
    }

    private String loadPrompt(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private Map<String, Object> buildOpenAiRequest(String userText, String systemPrompt) {
        // Structured Outputs Schema
        Map<String, Object> jsonSchema = Map.of(
                "name", "search_filters",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "radius", Map.of("type", "integer", "description", "Search radius in meters"),
                                "category", Map.of("type", List.of("string", "null"), "description", "Category name"),
                                "brand", Map.of("type", List.of("string", "null"), "description", "Brand name"),
                                "notes", Map.of("type", "string", "description", "Reasoning"),
                                "confidence", Map.of("type", "number", "description", "Confidence score")
                        ),
                        "required", List.of("radius", "category", "brand", "notes", "confidence"),
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
    private AiQueryRes parseResponse(Map<String, Object> response) {
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

            // content is a JSON string enforced by the schema
            JsonNode root = objectMapper.readTree(content);

            int radius = root.get("radius").asInt();
            String category = root.get("category").isNull() ? null : root.get("category").asText();
            String brand = root.get("brand").isNull() ? null : root.get("brand").asText();
            String notes = root.get("notes").asText();
            double confidence = root.get("confidence").asDouble();

            return new AiQueryRes(radius, category, brand, notes, confidence, false);

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response", e);
            throw new RuntimeException("Parsing failed", e);
        }
    }
}
