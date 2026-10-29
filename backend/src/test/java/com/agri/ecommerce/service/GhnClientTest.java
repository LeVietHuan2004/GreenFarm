package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agri.ecommerce.config.GhnProperties;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class GhnClientTest {
    private final GhnProperties properties = mock(GhnProperties.class);

    @Test void distinguishesMissingOrderFromOtherRejectedRequests() {
        when(properties.baseUrl()).thenReturn("https://dev-online-gateway.ghn.vn");
        GhnClient client = new GhnClient(properties);
        assertThat(client.gatewayError(error("{\"message\":\"Lỗi gọi API: corev2 - Đơn hàng không tồn tại\"}" )).getCode())
            .isEqualTo("GHN_ORDER_NOT_FOUND");
        assertThat(client.gatewayError(error("{\"code_message\":\"ORDER_NOT_FOUND\"}")).getCode())
            .isEqualTo("GHN_ORDER_NOT_FOUND");
        assertThat(client.gatewayError(error("{\"code_message\":\"CLIENT_NOT_OWNER_OF_SHOP\"}")).getCode())
            .isEqualTo("GHN_REQUEST_REJECTED");
        assertThat(client.gatewayError(error("")).getCode()).isEqualTo("GHN_REQUEST_REJECTED");
    }

    private HttpClientErrorException error(String body) {
        return HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
            body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
