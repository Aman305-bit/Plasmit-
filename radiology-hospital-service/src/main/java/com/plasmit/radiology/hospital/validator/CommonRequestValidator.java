package com.plasmit.radiology.hospital.validator;

import com.plasmit.radiology.hospital.common.exception.ApiException;
import com.plasmit.radiology.hospital.context.TenantContext;
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