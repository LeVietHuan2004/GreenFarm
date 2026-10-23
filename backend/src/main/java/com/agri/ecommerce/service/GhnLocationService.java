package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.response.GhnLocationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GhnLocationService {
    private final GhnClient client;
    public GhnLocationService(GhnClient client) { this.client = client; }

    public List<GhnLocationResponse> provinces() {
        return locations(client.get("/shiip/public-api/v3/master-data/province/all?offset=0&limit=200"));
    }

    public List<GhnLocationResponse> wards(int provinceId) {
        if (provinceId <= 0) throw invalidAddress();
        JsonNode data = client.get("/shiip/public-api/v3/master-data/ward/all-by-province-id?province_id="
            + provinceId + "&offset=0&limit=200");
        if (!data.isArray()) throw invalidAddress();
        return locations(data);
    }

    public GhnLocationResponse province(int provinceId) {
        return provinces().stream().filter(item -> item.id() == provinceId).findFirst().orElseThrow(this::invalidAddress);
    }

    public GhnLocationResponse ward(int provinceId, int wardId) {
        return wards(provinceId).stream().filter(item -> item.id() == wardId).findFirst().orElseThrow(this::invalidAddress);
    }

    private List<GhnLocationResponse> locations(JsonNode data) {
        if (!data.isArray()) throw invalidAddress();
        List<GhnLocationResponse> result = new ArrayList<>();
        for (JsonNode entry : data) {
            if (entry.path("status").asInt() == 1 && entry.path("_id").asInt() > 0
                && !entry.path("name").asText().isBlank())
                result.add(new GhnLocationResponse(entry.path("_id").asInt(), entry.path("name").asText()));
        }
        return result;
    }

    private ApplicationException invalidAddress() {
        return new ApplicationException(HttpStatus.BAD_REQUEST, "GHN_ADDRESS_INVALID", "Tỉnh hoặc phường/xã GHN không hợp lệ");
    }
}
