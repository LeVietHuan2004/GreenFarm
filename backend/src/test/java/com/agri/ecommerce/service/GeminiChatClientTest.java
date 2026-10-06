package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiChatClientTest {

    @Test
    void detectsResponseStoppedByOutputTokenLimit() {
        Map<String, Object> response = Map.of(
            "candidates", List.of(Map.of("finishReason", "MAX_TOKENS"))
        );

        assertThat(GeminiChatClient.reachedOutputLimit(response)).isTrue();
    }

    @Test
    void acceptsNormallyCompletedResponse() {
        Map<String, Object> response = Map.of(
            "candidates", List.of(Map.of("finishReason", "STOP"))
        );

        assertThat(GeminiChatClient.reachedOutputLimit(response)).isFalse();
    }
}
