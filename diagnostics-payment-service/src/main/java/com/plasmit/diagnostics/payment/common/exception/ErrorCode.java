package com.plasmit.diagnostics.payment.common.exception;

public enum ErrorCode {

    VALIDATION_ERROR,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    DUPLICATE_REQUEST,
    BUSINESS_RULE_FAILED,
    WORKFLOW_ERROR,
    TENANT_CONTEXT_MISSING,
    INTERNAL_ERROR
}