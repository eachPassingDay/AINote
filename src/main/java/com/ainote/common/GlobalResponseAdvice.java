package com.ainote.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

@RestControllerAdvice("com.ainote.controller")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public GlobalResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 如果已经是 Result 类型，无需再包装
        if (returnType.getParameterType().isAssignableFrom(Result.class)) {
            return false;
        }
        // StreamingResponseBody 类型无需包装
        if (returnType.getParameterType()
                .isAssignableFrom(StreamingResponseBody.class)) {
            return false;
        }
        // SseEmitter 类型无需包装
        if (SseEmitter.class
                .isAssignableFrom(returnType.getParameterType())) {
            return false;
        }
        // Flux 类型无需包装
        if (Flux.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {

        // 如果返回类型是 String，Spring 会直接转为字符串，需要手动将 Result 序列化为 JSON 字符串
        if (returnType.getParameterType().isAssignableFrom(String.class)) {
            try {
                return objectMapper.writeValueAsString(Result.success(body));
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "Failed to parse json");
            }
        }

        return Result.success(body);
    }
}
