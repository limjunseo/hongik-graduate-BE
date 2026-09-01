package com.hongik.graduate.BE.auth.security;

import java.io.IOException;
import java.time.Instant;

import com.hongik.graduate.BE.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        ErrorCode code = ErrorCode.FORBIDDEN;
        response.setStatus(code.status().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"timestamp\":\"" + Instant.now()
                + "\",\"status\":403,\"errorCode\":\"FORBIDDEN\""
                + ",\"message\":\"접근 권한이 없습니다.\",\"path\":\""
                + request.getRequestURI() + "\"}");
    }
}
