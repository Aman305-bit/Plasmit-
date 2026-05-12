package com.plasmit.hospital.core.validator;

import com.plasmit.hospital.core.exception.ApiException;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.BranchOption;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.DepartmentOption;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.HospitalProfile;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.ReferenceOption;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.SettingRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HospitalCoreValidator {

    private static final Logger log = LoggerFactory.getLogger(HospitalCoreValidator.class);

    public void validateTenantHospitalContext(Long tenantId, Long hospitalId) {

        if (tenantId == null || hospitalId == null) {
            log.warn(
                    "Hospital core validation failed. reason=TENANT_OR_HOSPITAL_MISSING tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (tenantId <= 0 || hospitalId <= 0) {
            log.warn(
                    "Hospital core validation failed. reason=INVALID_TENANT_OR_HOSPITAL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateHospitalProfile(
            HospitalProfile hospital,
            Long tenantId,
            Long hospitalId
    ) {

        validateTenantHospitalContext(tenantId, hospitalId);

        if (hospital == null) {
            log.warn(
                    "Hospital core validation failed. reason=HOSPITAL_PROFILE_NOT_FOUND tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Hospital profile not found.");
        }

        if (hospital.id() == null || hospital.id() <= 0) {
            log.warn(
                    "Hospital core validation failed. reason=INVALID_HOSPITAL_PROFILE_ID tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (!"ACTIVE".equalsIgnoreCase(hospital.status())) {
            log.warn(
                    "Hospital core validation failed. reason=HOSPITAL_INACTIVE tenantId={} hospitalId={} status={}",
                    tenantId,
                    hospitalId,
                    hospital.status()
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateBranchContext(Long branchId) {

        if (branchId != null && branchId <= 0) {
            log.warn("Hospital core validation failed. reason=INVALID_BRANCH_ID branchId={}", branchId);
            throw ApiException.badRequest("Invalid branch id.");
        }
    }

    public void validateBranches(
            List<BranchOption> branches,
            Long tenantId,
            Long hospitalId
    ) {

        if (branches == null) {
            log.warn(
                    "Hospital core validation failed. reason=BRANCH_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch branches.");
        }

        for (BranchOption branch : branches) {
            if (branch.id() == null || branch.id() <= 0) {
                log.warn(
                        "Hospital core validation failed. reason=INVALID_BRANCH_RECORD tenantId={} hospitalId={} branch={}",
                        tenantId,
                        hospitalId,
                        branch
                );
                throw ApiException.badRequest("Invalid branch data.");
            }
        }
    }

    public void validateDepartments(
            List<DepartmentOption> departments,
            Long tenantId,
            Long hospitalId
    ) {

        if (departments == null) {
            log.warn(
                    "Hospital core validation failed. reason=DEPARTMENT_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch departments.");
        }

        for (DepartmentOption department : departments) {
            if (department.id() == null || department.id() <= 0) {
                log.warn(
                        "Hospital core validation failed. reason=INVALID_DEPARTMENT_RECORD tenantId={} hospitalId={} department={}",
                        tenantId,
                        hospitalId,
                        department
                );
                throw ApiException.badRequest("Invalid department data.");
            }
        }
    }

    public void validateReferenceType(String referenceType) {

        if (referenceType == null || referenceType.isBlank()) {
            log.warn("Hospital core validation failed. reason=REFERENCE_TYPE_REQUIRED");
            throw ApiException.badRequest("Reference type is required.");
        }
    }

    public void validateReferenceValues(
            String referenceType,
            List<ReferenceOption> references,
            Long tenantId,
            Long hospitalId
    ) {

        validateReferenceType(referenceType);

        if (references == null) {
            log.warn(
                    "Hospital core validation failed. reason=REFERENCE_LIST_NULL referenceType={} tenantId={} hospitalId={}",
                    referenceType,
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch reference values.");
        }

        for (ReferenceOption reference : references) {
            if (reference.code() == null || reference.code().isBlank()) {
                log.warn(
                        "Hospital core validation failed. reason=INVALID_REFERENCE_CODE referenceType={} tenantId={} hospitalId={} reference={}",
                        referenceType,
                        tenantId,
                        hospitalId,
                        reference
                );
                throw ApiException.badRequest("Invalid reference data.");
            }
        }
    }

    public void validateSettings(
            List<SettingRecord> settings,
            Long tenantId,
            Long hospitalId
    ) {

        if (settings == null) {
            log.warn(
                    "Hospital core validation failed. reason=SETTINGS_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch hospital settings.");
        }

        for (SettingRecord setting : settings) {
            if (setting.key() == null || setting.key().isBlank()) {
                log.warn(
                        "Hospital core validation failed. reason=INVALID_SETTING_KEY tenantId={} hospitalId={} setting={}",
                        tenantId,
                        hospitalId,
                        setting
                );
                throw ApiException.badRequest("Invalid hospital setting data.");
            }
        }
    }
}