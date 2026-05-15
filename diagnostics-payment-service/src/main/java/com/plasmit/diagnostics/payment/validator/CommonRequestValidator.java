package com.plasmit.diagnostics.payment.validator;

import com.plasmit.diagnostics.payment.common.exception.ApiException;
import com.plasmit.diagnostics.payment.context.TenantContext;
import org.springframework.stereotype.Component;

@Component
public class CommonRequestValidator {

    public void validateTenantContext() {
        if (TenantContext.getTenantId() == null) {
            throw ApiException.tenantContextMissing("Tenant id missing from token.");
        }
    }

    public void validateHospitalContext() {
        if (TenantContext.getHospitalId() == null) {
            throw ApiException.tenantContextMissing("Hospital id missing from token.");
        }
    }

    public void validateBranchRequired() {
        String branchId = TenantContext.getBranchId();

        if (branchId == null || branchId.isBlank()) {
            throw ApiException.validation("X-Branch-Id header is required.");
        }
    }

    public void validateUserContext() {
        if (TenantContext.getUserId() == null) {
            throw ApiException.tenantContextMissing("User id missing from token.");
        }
    }

    public void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.validation("X-Idempotency-Key header is required.");
        }
    }
}