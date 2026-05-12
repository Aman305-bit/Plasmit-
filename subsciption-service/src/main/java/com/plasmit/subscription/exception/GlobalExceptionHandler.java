package com.plasmit.subscription.exception;

import com.plasmit.subscription.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<Map<String, Object>> errors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("field", fieldError.getField());
            error.put("message", fieldError.getDefaultMessage());
            error.put("rejectedValue", fieldError.getRejectedValue());
            errors.add(error);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("path", request.getRequestURI());
        data.put("errors", errors);

        log.warn("Validation failed. path={} errors={}", request.getRequestURI(), errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Validation failed.", data));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("path", request.getRequestURI());

        log.warn("Bad request. path={} message={}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), data));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("path", request.getRequestURI());

        log.error("Unexpected error. path={}", request.getRequestURI(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Internal server error.", data));
    }
}