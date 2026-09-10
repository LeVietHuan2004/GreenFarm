package com.agri.ecommerce.security;

import com.agri.ecommerce.common.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
        HttpServletResponse response,
        HttpStatus status,
        String code,
        String message,
        String path
    ) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            code,
            message,
            path,
            List.of()
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
