package com.agri.ecommerce.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter responseWriter;

    public RestAccessDeniedHandler(SecurityErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {
        responseWriter.write(
            response,
            HttpStatus.FORBIDDEN,
            "ACCESS_DENIED",
            "Ban khong co quyen truy cap tai nguyen nay",
            request.getRequestURI()
        );
    }
}
