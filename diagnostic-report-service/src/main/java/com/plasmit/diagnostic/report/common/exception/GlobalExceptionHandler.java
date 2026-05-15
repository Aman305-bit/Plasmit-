package com.plasmit.diagnostic.report.common.exception;

import com.plasmit.diagnostic.report.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleApiException(ApiException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", ex.getErrorCode().name());
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.failure(ex.getMessage(), error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> details = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", fieldError.getField());
            item.put("message", fieldError.getDefaultMessage());
            details.add(item);
        }

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", ErrorCode.VALIDATION_ERROR.name());
        error.put("details", details);

        return ResponseEntity.badRequest().body(ApiResponse.failure("Validation failed.", error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraint(ConstraintViolationException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", ErrorCode.VALIDATION_ERROR.name());
        error.put("message", ex.getMessage());

        return ResponseEntity.badRequest().body(ApiResponse.failure("Validation failed.", error));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", ErrorCode.VALIDATION_ERROR.name());
        error.put("field", ex.getName());
        error.put("message", "Invalid value for parameter: " + ex.getName());

        return ResponseEntity.badRequest().body(ApiResponse.failure("Invalid request parameter.", error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGeneric(Exception ex) {
        log.error("Unhandled exception occurred.", ex);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", ErrorCode.INTERNAL_ERROR.name());
        error.put("message", "Something went wrong.");

        return ResponseEntity.internalServerError().body(ApiResponse.failure("Something went wrong.", error));
    }
}