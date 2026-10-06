package com.agri.ecommerce.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CorsConfigTest {
    @Test
    void guestCartPreflightAllowsGuestTokenHeader() {
        var source = new CorsConfig().corsConfigurationSource(List.of("http://localhost:3000"));
        var request = new MockHttpServletRequest("OPTIONS", "/api/public/guest/cart");
        var configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(configuration.checkHeaders(List.of("x-guest-token", "content-type")))
            .containsExactly("x-guest-token", "content-type");
    }
}
