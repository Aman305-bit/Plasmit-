package com.plasmit.diagnostic.quality.validator;

import com.plasmit.diagnostic.quality.common.exception.ApiException;
import com.plasmit.diagnostic.quality.context.TenantContext;
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

    public void validateUserContext() {
        if (TenantContext.getUserId() == null) {
            throw ApiException.tenantContextMissing("User id missing from token.");
        }
    }

    public void validateBranchRequired() {
        if (TenantContext.getBranchId() == null || TenantContext.getBranchId().isBlank()) {
            throw ApiException.validation("X-Branch-Id header is required.");
        }
    }
}