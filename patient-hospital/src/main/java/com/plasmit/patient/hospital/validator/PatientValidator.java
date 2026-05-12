package com.plasmit.patient.hospital.validator;

import java.util.List;

import com.plasmit.patient.hospital.exception.ApiException;
import com.plasmit.patient.hospital.repository.PatientRepository.PatientRecord;
import com.plasmit.patient.hospital.service.PatientService.CreatePatientRequest;
import com.plasmit.patient.hospital.service.PatientService.UpdatePatientRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PatientValidator {

    private static final Logger log = LoggerFactory.getLogger(PatientValidator.class);

    public void validateTenantHospitalContext(Long tenantId, Long hospitalId) {

        if (tenantId == null || hospitalId == null) {
            log.warn(
                    "Patient validation failed. reason=TENANT_OR_HOSPITAL_MISSING tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (tenantId <= 0 || hospitalId <= 0) {
            log.warn(
                    "Patient validation failed. reason=INVALID_TENANT_OR_HOSPITAL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateBranchContext(Long branchId) {

        if (branchId != null && branchId <= 0) {
            log.warn("Patient validation failed. reason=INVALID_BRANCH_ID branchId={}", branchId);
            throw ApiException.badRequest("Invalid branch id.");
        }
    }

    public int validatePage(Integer page) {

        if (page == null || page < 1) {
            return 1;
        }

        return page;
    }

    public int validateSize(Integer size) {

        if (size == null || size < 1) {
            return 20;
        }

        if (size > 100) {
            log.warn("Patient validation warning. reason=PAGE_SIZE_LIMIT_EXCEEDED requestedSize={}", size);
            return 100;
        }

        return size;
    }

    public void validateStatusFilter(String status) {

        if (status == null || status.isBlank()) {
            return;
        }

        if (!status.equalsIgnoreCase("ACTIVE") && !status.equalsIgnoreCase("INACTIVE")) {
            log.warn("Patient validation failed. reason=INVALID_STATUS status={}", status);
            throw ApiException.badRequest("Status must be ACTIVE or INACTIVE.");
        }
    }

    public void validatePatientId(Long patientId) {

        if (patientId == null || patientId <= 0) {
            log.warn("Patient validation failed. reason=INVALID_PATIENT_ID patientId={}", patientId);
            throw ApiException.badRequest("Invalid patient id.");
        }
    }

    public void validatePatientFound(
            PatientRecord patient,
            Long patientId,
            Long tenantId,
            Long hospitalId
    ) {

        if (patient == null) {
            log.warn(
                    "Patient validation failed. reason=PATIENT_NOT_FOUND patientId={} tenantId={} hospitalId={}",
                    patientId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Patient not found.");
        }
    }

    public void validatePatientsList(
            List<PatientRecord> patients,
            Long tenantId,
            Long hospitalId
    ) {

        if (patients == null) {
            log.warn(
                    "Patient validation failed. reason=PATIENT_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch patients.");
        }

        for (PatientRecord patient : patients) {
            if (patient.id() == null || patient.id() <= 0) {
                log.warn(
                        "Patient validation failed. reason=INVALID_PATIENT_RECORD tenantId={} hospitalId={} patient={}",
                        tenantId,
                        hospitalId,
                        patient
                );
                throw ApiException.badRequest("Invalid patient data.");
            }
        }
    }

    public void validateSearchQuery(String query) {

        if (query == null || query.isBlank()) {
            log.warn("Patient validation failed. reason=SEARCH_QUERY_REQUIRED");
            throw ApiException.badRequest("Search query is required.");
        }

        if (query.trim().length() < 2) {
            log.warn("Patient validation failed. reason=SEARCH_QUERY_TOO_SHORT query={}", query);
            throw ApiException.badRequest("Search query must be at least 2 characters.");
        }
    }

    public void validatePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            log.warn("Patient validation failed. reason=PHONE_REQUIRED");
            throw ApiException.badRequest("Phone is required.");
        }

        if (!phone.matches("^[0-9]{10,15}$")) {
            log.warn("Patient validation failed. reason=INVALID_PHONE phone={}", phone);
            throw ApiException.badRequest("Phone must be 10 to 15 digits.");
        }
    }

    public void validateCreateRequest(CreatePatientRequest request) {

        if (request == null) {
            log.warn("Patient validation failed. reason=CREATE_REQUEST_NULL");
            throw ApiException.badRequest("Patient request is required.");
        }

        validatePhone(request.phone());

        validateGender(request.gender());
    }

    public void validateUpdateRequest(UpdatePatientRequest request) {

        if (request == null) {
            log.warn("Patient validation failed. reason=UPDATE_REQUEST_NULL");
            throw ApiException.badRequest("Patient request is required.");
        }

        validatePhone(request.phone());

        validateGender(request.gender());
    }

    public void validateGender(String gender) {

        if (gender == null || gender.isBlank()) {
            return;
        }

        if (!gender.equalsIgnoreCase("MALE")
                && !gender.equalsIgnoreCase("FEMALE")
                && !gender.equalsIgnoreCase("OTHER")) {

            log.warn("Patient validation failed. reason=INVALID_GENDER gender={}", gender);
            throw ApiException.badRequest("Gender must be MALE, FEMALE or OTHER.");
        }
    }

    public void validateDuplicatePhone(
            boolean exists,
            String phone,
            Long tenantId,
            Long hospitalId
    ) {

        if (exists) {
            log.warn(
                    "Patient validation failed. reason=DUPLICATE_PHONE tenantId={} hospitalId={} phone={}",
                    tenantId,
                    hospitalId,
                    phone
            );
            throw ApiException.conflict("Patient already exists with this phone number.");
        }
    }

    public void validateDuplicatePhoneForUpdate(
            boolean exists,
            String phone,
            Long patientId,
            Long tenantId,
            Long hospitalId
    ) {

        if (exists) {
            log.warn(
                    "Patient validation failed. reason=DUPLICATE_PHONE_FOR_UPDATE patientId={} tenantId={} hospitalId={} phone={}",
                    patientId,
                    tenantId,
                    hospitalId,
                    phone
            );
            throw ApiException.conflict("Another patient already exists with this phone number.");
        }
    }

    public void validateUpdateCount(
            int updated,
            Long patientId,
            Long tenantId,
            Long hospitalId
    ) {

        if (updated == 0) {
            log.warn(
                    "Patient validation failed. reason=PATIENT_UPDATE_FAILED_OR_NOT_FOUND patientId={} tenantId={} hospitalId={}",
                    patientId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Patient not found.");
        }
    }

    public void validateDeleteCount(
            int updated,
            Long patientId,
            Long tenantId,
            Long hospitalId
    ) {

        if (updated == 0) {
            log.warn(
                    "Patient validation failed. reason=PATIENT_DELETE_FAILED_OR_NOT_FOUND patientId={} tenantId={} hospitalId={}",
                    patientId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Patient not found.");
        }
    }
}