package com.plasmit.patient.hospital.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.plasmit.patient.hospital.exception.ApiException;
import com.plasmit.patient.hospital.repository.PatientRepository;
import com.plasmit.patient.hospital.repository.PatientRepository.PatientRecord;
import com.plasmit.patient.hospital.security.SecurityConfig.CurrentUser;
import com.plasmit.patient.hospital.security.SecurityConfig.TenantContext;
import com.plasmit.patient.hospital.validator.PatientValidator;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository repository;
    private final PatientValidator validator;

    public PatientService(
            PatientRepository repository,
            PatientValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<PatientRecord> listPatients(String query, String status, Integer page, Integer size) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateStatusFilter(status);

        requirePermission(currentUser, "patients.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        log.info("List patients request. userId={} tenantId={} hospitalId={} branchId={} page={} size={}",
                currentUser.getUserId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                safePage,
                safeSize);

        List<PatientRecord> patients = repository.findPatients(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query,
                status,
                safeSize,
                offset
        );

        validator.validatePatientsList(
                patients,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return patients;
    }

    public PatientRecord getPatient(Long patientId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePatientId(patientId);

        requirePermission(currentUser, "patients.view");

        log.info("Patient detail request. patientId={} tenantId={} hospitalId={}",
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        PatientRecord patient = repository.findById(
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validatePatientFound(
                patient,
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return patient;
    }

    @Transactional
    public PatientRecord createPatient(CreatePatientRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateRequest(request);

        requirePermission(currentUser, "patients.create");

        log.info("Create patient request. tenantId={} hospitalId={} branchId={} phone={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.phone());

        boolean phoneExists = repository.existsByPhone(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.phone()
        );

        validator.validateDuplicatePhone(
                phoneExists,
                request.phone(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        String patientCode = repository.generateNextPatientCode(currentUser.getHospitalId());

        Long patientId = repository.createPatient(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                patientCode,
                request
        );

        log.info("Patient created successfully. patientId={} patientCode={} tenantId={} hospitalId={}",
                patientId,
                patientCode,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        return getPatient(patientId);
    }

    @Transactional
    public PatientRecord updatePatient(Long patientId, UpdatePatientRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePatientId(patientId);
        validator.validateUpdateRequest(request);

        requirePermission(currentUser, "patients.update");

        log.info("Update patient request. patientId={} tenantId={} hospitalId={}",
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        PatientRecord existing = repository.findById(
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validatePatientFound(
                existing,
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        boolean duplicatePhone = repository.existsByPhoneExcludingId(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.phone(),
                existing.id()
        );

        validator.validateDuplicatePhoneForUpdate(
                duplicatePhone,
                request.phone(),
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        int updated = repository.updatePatient(
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request
        );

        validator.validateUpdateCount(
                updated,
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Patient updated successfully. patientId={}", patientId);

        return getPatient(patientId);
    }

    public DuplicateCheckResponse duplicateCheck(String phone) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validatePhone(phone);

        requirePermission(currentUser, "patients.view");

        boolean exists = repository.existsByPhone(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                phone
        );

        return new DuplicateCheckResponse(phone, exists);
    }

    public List<PatientRecord> search(String query) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateSearchQuery(query);

        requirePermission(currentUser, "patients.view");

        List<PatientRecord> patients = repository.findPatients(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query,
                "ACTIVE",
                20,
                0
        );

        validator.validatePatientsList(
                patients,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return patients;
    }

    @Transactional
    public Map<String, Object> deletePatient(Long patientId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePatientId(patientId);

        requirePermission(currentUser, "patients.update");

        int updated = repository.softDelete(
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId()
        );

        validator.validateDeleteCount(
                updated,
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Patient deleted successfully. patientId={} userId={}",
                patientId,
                currentUser.getUserId());

        return Map.of(
                "patientId", patientId,
                "deleted", true
        );
    }

    private CurrentUser requireUser() {

        CurrentUser currentUser = TenantContext.get();

        if (currentUser == null) {
            throw ApiException.unauthorized("Authentication required.");
        }

        return currentUser;
    }

    private void requirePermission(CurrentUser currentUser, String permission) {

        if (!currentUser.hasPermission(permission)) {
            log.warn("Permission denied. userId={} tenantId={} hospitalId={} permission={}",
                    currentUser.getUserId(),
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    permission);

            throw ApiException.forbidden("Permission denied.");
        }
    }

    public record CreatePatientRequest(
            @NotBlank(message = "First name is required.")
            @Size(max = 100, message = "First name must be within 100 characters.")
            String firstName,

            @Size(max = 100, message = "Last name must be within 100 characters.")
            String lastName,

            String gender,

            LocalDate dateOfBirth,

            Integer ageYears,

            String bloodGroup,

            @NotBlank(message = "Phone is required.")
            @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10 to 15 digits.")
            String phone,

            String alternatePhone,

            @Email(message = "Email must be valid.")
            String email,

            String address,
            String city,
            String state,
            String country,
            String pincode,

            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelation,

            String sourceChannel,
            String payerType
    ) {
    }

    public record UpdatePatientRequest(
            @NotBlank(message = "First name is required.")
            @Size(max = 100, message = "First name must be within 100 characters.")
            String firstName,

            @Size(max = 100, message = "Last name must be within 100 characters.")
            String lastName,

            String gender,

            LocalDate dateOfBirth,

            Integer ageYears,

            String bloodGroup,

            @NotBlank(message = "Phone is required.")
            @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10 to 15 digits.")
            String phone,

            String alternatePhone,

            @Email(message = "Email must be valid.")
            String email,

            String address,
            String city,
            String state,
            String country,
            String pincode,

            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelation,

            String sourceChannel,
            String payerType
    ) {
    }

    public record DuplicateCheckResponse(
            String phone,
            boolean exists
    ) {
    }
}