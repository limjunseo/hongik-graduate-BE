package com.hongik.graduate.BE.auth.security;

import java.io.IOException;
import java.time.Instant;

import com.hongik.graduate.BE.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        ErrorCode code = exception.getMessage() != null
                && exception.getMessage().toLowerCase().contains("expired")
                ? ErrorCode.ACCESS_TOKEN_EXPIRED : ErrorCode.INVALID_ACCESS_TOKEN;
        response.setStatus(code.status().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"timestamp\":\"" + Instant.now()
                + "\",\"status\":" + code.status().value()
                + ",\"errorCode\":\"" + code.name()
                + "\",\"message\":\"" + code.message()
                + "\",\"path\":\"" + request.getRequestURI() + "\"}");
    }
}
