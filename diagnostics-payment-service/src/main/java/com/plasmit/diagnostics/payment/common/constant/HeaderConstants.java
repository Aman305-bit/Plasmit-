package com.plasmit.diagnostics.payment.common.constant;

public final class HeaderConstants {

    private HeaderConstants() {
    }

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String X_REQUEST_ID = "X-Request-Id";
    public static final String X_BRANCH_ID = "X-Branch-Id";
    public static final String X_IDEMPOTENCY_KEY = "X-Idempotency-Key";
}