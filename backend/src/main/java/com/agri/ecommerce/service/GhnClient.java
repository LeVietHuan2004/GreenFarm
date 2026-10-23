package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.config.GhnProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GhnClient {
    private final GhnProperties properties;
    private final RestClient client;
    private final ObjectMapper json = new ObjectMapper();

    public GhnClient(GhnProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(10000);
        this.client = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    }

    public JsonNode get(String path) {
        properties.requireEnabled();
        try {
            return data(client.get().uri(path).header("Token", properties.token())
                .header("ShopId", String.valueOf(properties.shopId())).retrieve().body(String.class));
        } catch (RestClientResponseException exception) {
            throw gatewayError(exception);
        } catch (RestClientException exception) {
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_UNAVAILABLE", "Không kết nối được GHN. Vui lòng thử lại");
        }
    }

    public JsonNode post(String path, Map<String, Object> body) {
        properties.requireEnabled();
        try {
            return data(client.post().uri(path).header("Token", properties.token())
                .header("ShopId", String.valueOf(properties.shopId())).body(body).retrieve().body(String.class));
        } catch (RestClientResponseException exception) {
            throw gatewayError(exception);
        } catch (RestClientException exception) {
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_UNAVAILABLE", "Không kết nối được GHN. Vui lòng thử lại");
        }
    }

    private JsonNode data(String body) {
        try {
            JsonNode response = body == null ? null : json.readTree(body);
            if (response == null || response.path("code").asInt() != 200 || response.path("data").isMissingNode())
                throw new ApplicationException(HttpStatus.BAD_GATEWAY, "GHN_INVALID_RESPONSE", "GHN trả về dữ liệu không hợp lệ");
            return response.path("data");
        } catch (JsonProcessingException exception) {
            throw new ApplicationException(HttpStatus.BAD_GATEWAY, "GHN_INVALID_RESPONSE", "GHN trả về dữ liệu không hợp lệ");
        }
    }

    private ApplicationException gatewayError(RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 401)
            return new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_AUTH_FAILED", "GHN từ chối token hoặc ShopId staging");
        if (exception.getStatusCode().is4xxClientError())
            return new ApplicationException(HttpStatus.BAD_REQUEST, "GHN_REQUEST_REJECTED", "GHN không hỗ trợ thông tin vận chuyển này");
        return new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_UNAVAILABLE", "GHN đang tạm thời không phản hồi");
    }
}
