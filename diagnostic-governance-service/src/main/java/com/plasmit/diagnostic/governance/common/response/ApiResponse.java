package com.plasmit.diagnostic.governance.common.response;

import com.plasmit.diagnostic.governance.context.TenantContext;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ApiMeta meta;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data, ApiMeta meta) {
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
                new ApiMeta(TenantContext.getRequestId())
        );
    }

    public static <T> ApiResponse<T> failure(String message, T data) {
        return new ApiResponse<>(
                false,
                message,
                data,
                new ApiMeta(TenantContext.getRequestId())
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

    public ApiMeta getMeta() {
        return meta;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setMeta(ApiMeta meta) {
        this.meta = meta;
    }
}