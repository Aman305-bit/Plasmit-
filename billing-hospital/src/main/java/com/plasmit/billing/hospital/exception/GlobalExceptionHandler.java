package com.plasmit.billing.hospital.exception;

import java.util.ArrayList;
import java.util.List;

import com.plasmit.billing.hospital.common.ApiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        List<ApiResponse.FieldError> errors = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.add(new ApiResponse.FieldError(error.getField(), error.getDefaultMessage()))
        );

        log.warn("Validation failed. errorCount={}", errors.size());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiResponse.ErrorResponse("Validation failed.", "VALIDATION_ERROR", errors));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleApiException(ApiException ex) {

        log.warn("API exception. code={}, message={}", ex.getCode(), ex.getMessage());

        return ResponseEntity.status(ex.getStatus())
                .body(new ApiResponse.ErrorResponse(ex.getMessage(), ex.getCode(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleException(Exception ex) {

        log.error("Unhandled server error. error={}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse.ErrorResponse(
                        "Internal server error.",
                        "INTERNAL_SERVER_ERROR",
                        List.of()
                ));
    }
}