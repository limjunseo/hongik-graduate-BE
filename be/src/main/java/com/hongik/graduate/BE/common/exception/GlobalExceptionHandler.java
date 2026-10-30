package com.hongik.graduate.BE.common.exception;

import java.time.Instant;

import com.hongik.graduate.BE.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return response(exception.errorCode(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        ErrorCode errorCode = fieldError != null && "email".equals(fieldError.getField())
                ? ErrorCode.INVALID_EMAIL
                : fieldError != null && "password".equals(fieldError.getField())
                        ? ErrorCode.INVALID_PASSWORD
                        : ErrorCode.INVALID_REQUEST;
        return response(errorCode, request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return response(ErrorCode.EMAIL_ALREADY_EXISTS, request.getRequestURI());
    }

    @ExceptionHandler(com.hongik.graduate.ai.common.AiProviderException.class)
    public ResponseEntity<ErrorResponse> handleAi(
            com.hongik.graduate.ai.common.AiProviderException exception,
            HttpServletRequest request) {
        ErrorCode code = switch (exception.code()) {
            case DISABLED -> ErrorCode.AI_DISABLED;
            case INVALID_REQUEST -> ErrorCode.INVALID_REQUEST;
            case TEMPLATE_NOT_FOUND -> ErrorCode.INTERNAL_SERVER_ERROR;
            case INVALID_OUTPUT -> ErrorCode.AI_INVALID_OUTPUT;
            case PROVIDER_FAILURE -> ErrorCode.AI_PROVIDER_FAILURE;
            case RATE_LIMITED -> ErrorCode.AI_RATE_LIMITED;
            case TIMEOUT -> ErrorCode.AI_TIMEOUT;
        };
        return response(code, request.getRequestURI());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        return response(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> response(ErrorCode errorCode, String path) {
        return ResponseEntity.status(errorCode.status()).body(new ErrorResponse(
                Instant.now(),
                errorCode.status().value(),
                errorCode.name(),
                errorCode.message(),
                path
        ));
    }
}
