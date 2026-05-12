package com.plasmit.pharmacy.sales.hospital.common;

import java.time.Instant;
import java.util.List;

import org.slf4j.MDC;

public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final Meta meta;

    public ApiResponse(boolean success, String message, T data, Meta meta) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                message,
                data,
                new Meta(MDC.get("requestId"), Instant.now())
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Meta getMeta() {
        return meta;
    }

    public static class Meta {
        private final String requestId;
        private final Instant timestamp;

        public Meta(String requestId, Instant timestamp) {
            this.requestId = requestId;
            this.timestamp = timestamp;
        }

        public String getRequestId() {
            return requestId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }

    public static class ErrorResponse {
        private final boolean success;
        private final String message;
        private final ErrorBody error;
        private final Meta meta;

        public ErrorResponse(String message, String code, List<FieldError> details) {
            this.success = false;
            this.message = message;
            this.error = new ErrorBody(code, details);
            this.meta = new Meta(MDC.get("requestId"), Instant.now());
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public ErrorBody getError() {
            return error;
        }

        public Meta getMeta() {
            return meta;
        }
    }

    public static class ErrorBody {
        private final String code;
        private final List<FieldError> details;

        public ErrorBody(String code, List<FieldError> details) {
            this.code = code;
            this.details = details;
        }

        public String getCode() {
            return code;
        }

        public List<FieldError> getDetails() {
            return details;
        }
    }

    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}