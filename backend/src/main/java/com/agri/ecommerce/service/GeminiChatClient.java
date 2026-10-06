package com.agri.ecommerce.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class GeminiChatClient {
    private static final Logger log = LoggerFactory.getLogger(GeminiChatClient.class);
    private static final Pattern MODEL_NAME = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private final String apiKey;
    private final String model;
    private final RestClient client;

    public GeminiChatClient(@Value("${app.chat.gemini.api-key:}") String apiKey,
                            @Value("${app.chat.gemini.model:gemini-3.5-flash}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || !MODEL_NAME.matcher(model.trim()).matches() ? "gemini-3.5-flash" : model.trim();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
    }

    public Optional<String> answer(String instructions, String input) {
        if (apiKey.isBlank()) return Optional.empty();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("systemInstruction", Map.of("parts", List.of(Map.of("text", instructions))));
        request.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", input)))));
        request.put("generationConfig", Map.of("maxOutputTokens", 2_000, "temperature", 0.2));

        try {
            Map<?, ?> response = client.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);
            if (reachedOutputLimit(response)) {
                log.warn("Gemini stopped because it reached the output token limit; using the public catalog fallback");
                return Optional.empty();
            }
            return extractText(response).map(String::trim).filter(value -> !value.isBlank());
        } catch (RestClientResponseException exception) {
            log.warn("Gemini chat request was rejected with HTTP {}; using the public catalog fallback", exception.getStatusCode().value());
            return Optional.empty();
        } catch (RuntimeException exception) {
            // Never log the request or response because both may contain customer conversation content.
            log.warn("Gemini chat request failed; using the public catalog fallback");
            return Optional.empty();
        }
    }

    private Optional<String> extractText(Map<?, ?> response) {
        if (response == null || !(response.get("candidates") instanceof List<?> candidates)) return Optional.empty();
        StringBuilder text = new StringBuilder();
        for (Object candidate : candidates) {
            if (!(candidate instanceof Map<?, ?> candidateMap) || !(candidateMap.get("content") instanceof Map<?, ?> content)
                || !(content.get("parts") instanceof List<?> parts)) continue;
            for (Object part : parts) {
                if (part instanceof Map<?, ?> partMap && partMap.get("text") instanceof String value) text.append(value);
            }
        }
        return text.isEmpty() ? Optional.empty() : Optional.of(text.toString());
    }

    static boolean reachedOutputLimit(Map<?, ?> response) {
        if (response == null || !(response.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()) {
            return false;
        }
        Object first = candidates.getFirst();
        if (!(first instanceof Map<?, ?> candidate)) return false;
        Object reason = candidate.get("finishReason");
        return reason instanceof String value && "MAX_TOKENS".equalsIgnoreCase(value);
    }
}
