package com.plasmit.diagnostic.report.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ErrorCode errorCode;

    public ApiException(HttpStatus httpStatus, ErrorCode errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, message);
    }

    public static ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message);
    }

    public static ApiException business(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BUSINESS_RULE_FAILED, message);
    }

    public static ApiException workflow(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.WORKFLOW_ERROR, message);
    }

    public static ApiException tenantContextMissing(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.TENANT_CONTEXT_MISSING, message);
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}