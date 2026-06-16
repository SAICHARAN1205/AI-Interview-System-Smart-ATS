package com.aihiringplatform.backend.config;

import com.aihiringplatform.backend.dto.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.aihiringplatform.backend.controller")
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public GlobalResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // Exclude specific endpoints (e.g., swagger, api-docs)
        String path = request.getURI().getPath();
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
            return body;
        }

        // If it's already an ApiResponse, return as is
        if (body instanceof ApiResponse) {
            return body;
        }

        // Pass Spring Page/Slice responses through unwrapped so the frontend
        // receives the full pagination structure (content, totalPages, last, first, etc.)
        if (body instanceof Page || body instanceof Slice) {
            return body;
        }

        // Skip binary data, resources, byte arrays
        if (body instanceof byte[] || body instanceof Resource) {
            return body;
        }

        if (selectedContentType.includes(MediaType.TEXT_PLAIN)) {
            return body;
        }

        // If we get a String return, we must serialize the ApiResponse ourselves
        // because StringHttpMessageConverter will try to cast it to String and fail
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(ApiResponse.success("Success", body));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error wrapping string response", e);
            }
        }

        // Wrap everything else in a successful ApiResponse
        return ApiResponse.success("Success", body);
    }
}
