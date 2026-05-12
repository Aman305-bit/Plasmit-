package com.plasmit.doctor.hospital.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.plasmit.doctor.hospital.exception.ApiException;
import com.plasmit.doctor.hospital.repository.DoctorRepository;
import com.plasmit.doctor.hospital.repository.DoctorRepository.AvailabilityRecord;
import com.plasmit.doctor.hospital.repository.DoctorRepository.DoctorRecord;
import com.plasmit.doctor.hospital.security.SecurityConfig.CurrentUser;
import com.plasmit.doctor.hospital.security.SecurityConfig.TenantContext;
import com.plasmit.doctor.hospital.validator.DoctorValidator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository repository;
    private final DoctorValidator validator;

    public DoctorService(
            DoctorRepository repository,
            DoctorValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<DoctorRecord> listDoctors(String query, String status, Integer page, Integer size) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateStatusFilter(status);

        requirePermission(currentUser, "doctors.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        log.info("List doctors request. userId={} tenantId={} hospitalId={} branchId={} page={} size={}",
                currentUser.getUserId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                safePage,
                safeSize);

        List<DoctorRecord> doctors = repository.findDoctors(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query,
                status,
                safeSize,
                offset
        );

        validator.validateDoctorList(
                doctors,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return doctors;
    }

    public DoctorRecord getDoctor(Long doctorId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateDoctorId(doctorId);

        requirePermission(currentUser, "doctors.view");

        log.info("Doctor detail request. doctorId={} tenantId={} hospitalId={}",
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        DoctorRecord doctor = repository.findById(
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateDoctorFound(
                doctor,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return doctor;
    }

    @Transactional
    public DoctorRecord createDoctor(CreateDoctorRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateRequest(request);

        requirePermission(currentUser, "doctors.create");

        log.info("Create doctor request. tenantId={} hospitalId={} branchId={} phone={} registration={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.phone(),
                request.registrationNumber());

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

        boolean registrationExists = repository.existsByRegistration(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.registrationNumber()
        );

        validator.validateDuplicateRegistration(
                registrationExists,
                request.registrationNumber(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        String doctorCode = repository.generateNextDoctorCode(currentUser.getHospitalId());

        Long doctorId = repository.createDoctor(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getDepartmentId(),
                currentUser.getUserId(),
                doctorCode,
                request
        );

        log.info("Doctor created successfully. doctorId={} doctorCode={} tenantId={} hospitalId={}",
                doctorId,
                doctorCode,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        return getDoctor(doctorId);
    }

    @Transactional
    public DoctorRecord updateDoctor(Long doctorId, UpdateDoctorRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateDoctorId(doctorId);
        validator.validateUpdateRequest(request);

        requirePermission(currentUser, "doctors.update");

        log.info("Update doctor request. doctorId={} tenantId={} hospitalId={}",
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        DoctorRecord existing = repository.findById(
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateDoctorFound(
                existing,
                doctorId,
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
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        boolean duplicateRegistration = repository.existsByRegistrationExcludingId(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.registrationNumber(),
                existing.id()
        );

        validator.validateDuplicateRegistrationForUpdate(
                duplicateRegistration,
                request.registrationNumber(),
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        int updated = repository.updateDoctor(
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request
        );

        validator.validateUpdateCount(
                updated,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Doctor updated successfully. doctorId={}", doctorId);

        return getDoctor(doctorId);
    }

    @Transactional
    public Map<String, Object> deleteDoctor(Long doctorId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateDoctorId(doctorId);

        requirePermission(currentUser, "doctors.archive");

        log.info("Archive doctor request. doctorId={} userId={}",
                doctorId,
                currentUser.getUserId());

        int updated = repository.softDelete(
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId()
        );

        validator.validateDeleteCount(
                updated,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Doctor archived successfully. doctorId={}", doctorId);

        return Map.of(
                "doctorId", doctorId,
                "archived", true
        );
    }

    public List<DoctorRecord> search(String query) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateSearchQuery(query);

        requirePermission(currentUser, "doctors.view");

        log.info("Doctor search request. query={} tenantId={} hospitalId={}",
                query,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        List<DoctorRecord> doctors = repository.findDoctors(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query,
                "ACTIVE",
                20,
                0
        );

        validator.validateDoctorList(
                doctors,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return doctors;
    }

    public List<AvailabilityRecord> getAvailability(Long doctorId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateDoctorId(doctorId);

        requirePermission(currentUser, "doctors.view");

        DoctorRecord doctor = repository.findById(
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateDoctorFound(
                doctor,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Doctor availability request. doctorId={} tenantId={} hospitalId={}",
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        List<AvailabilityRecord> availability = repository.findAvailability(
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        );

        validator.validateAvailabilityList(
                availability,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return availability;
    }

    @Transactional
    public List<AvailabilityRecord> updateAvailability(Long doctorId, UpdateAvailabilityRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateDoctorId(doctorId);
        validator.validateAvailabilityRequest(request);

        requirePermission(currentUser, "doctors.availability.manage");

        DoctorRecord doctor = repository.findById(
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateDoctorFound(
                doctor,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Update doctor availability request. doctorId={} items={} userId={}",
                doctorId,
                request.items().size(),
                currentUser.getUserId());

        for (AvailabilityItemRequest item : request.items()) {
            repository.upsertAvailability(
                    doctorId,
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    currentUser.getBranchId(),
                    currentUser.getUserId(),
                    item
            );
        }

        log.info("Doctor availability updated successfully. doctorId={}", doctorId);

        return getAvailability(doctorId);
    }

    @Transactional
    public DoctorRecord updateConsultationFee(Long doctorId, UpdateFeeRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateDoctorId(doctorId);
        validator.validateFeeRequest(request);

        requirePermission(currentUser, "doctors.fee.manage");

        DoctorRecord doctor = repository.findById(
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateDoctorFound(
                doctor,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Update doctor fee request. doctorId={} consultationFee={} followupFee={} emergencyFee={}",
                doctorId,
                request.consultationFee(),
                request.followupFee(),
                request.emergencyFee());

        int updated = repository.updateConsultationFee(
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request.consultationFee(),
                request.followupFee(),
                request.emergencyFee()
        );

        validator.validateUpdateCount(
                updated,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Doctor fee updated successfully. doctorId={}", doctorId);

        return getDoctor(doctorId);
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

    public record CreateDoctorRequest(
            @NotBlank(message = "First name is required.")
            @Size(max = 100, message = "First name must be within 100 characters.")
            String firstName,

            @Size(max = 100, message = "Last name must be within 100 characters.")
            String lastName,

            String gender,

            LocalDate dateOfBirth,

            @NotBlank(message = "Phone is required.")
            @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10 to 15 digits.")
            String phone,

            String alternatePhone,

            @Email(message = "Email must be valid.")
            String email,

            String qualification,
            String specialization,
            String registrationNumber,
            Integer experienceYears,

            @DecimalMin(value = "0.0", message = "Consultation fee cannot be negative.")
            BigDecimal consultationFee,

            @DecimalMin(value = "0.0", message = "Follow-up fee cannot be negative.")
            BigDecimal followupFee,

            @DecimalMin(value = "0.0", message = "Emergency fee cannot be negative.")
            BigDecimal emergencyFee,

            Integer consultationDurationMinutes,

            String address,
            String city,
            String state,
            String country,
            String pincode,
            String profilePhotoUrl,
            String notes
    ) {
    }

    public record UpdateDoctorRequest(
            @NotBlank(message = "First name is required.")
            @Size(max = 100, message = "First name must be within 100 characters.")
            String firstName,

            @Size(max = 100, message = "Last name must be within 100 characters.")
            String lastName,

            String gender,

            LocalDate dateOfBirth,

            @NotBlank(message = "Phone is required.")
            @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10 to 15 digits.")
            String phone,

            String alternatePhone,

            @Email(message = "Email must be valid.")
            String email,

            String qualification,
            String specialization,
            String registrationNumber,
            Integer experienceYears,

            @DecimalMin(value = "0.0", message = "Consultation fee cannot be negative.")
            BigDecimal consultationFee,

            @DecimalMin(value = "0.0", message = "Follow-up fee cannot be negative.")
            BigDecimal followupFee,

            @DecimalMin(value = "0.0", message = "Emergency fee cannot be negative.")
            BigDecimal emergencyFee,

            Integer consultationDurationMinutes,

            String address,
            String city,
            String state,
            String country,
            String pincode,
            String profilePhotoUrl,
            String notes
    ) {
    }

    public record UpdateFeeRequest(
            @NotNull(message = "Consultation fee is required.")
            @DecimalMin(value = "0.0", message = "Consultation fee cannot be negative.")
            BigDecimal consultationFee,

            @NotNull(message = "Follow-up fee is required.")
            @DecimalMin(value = "0.0", message = "Follow-up fee cannot be negative.")
            BigDecimal followupFee,

            @NotNull(message = "Emergency fee is required.")
            @DecimalMin(value = "0.0", message = "Emergency fee cannot be negative.")
            BigDecimal emergencyFee
    ) {
    }

    public record UpdateAvailabilityRequest(
            @NotEmpty(message = "Availability items are required.")
            List<@Valid AvailabilityItemRequest> items
    ) {
    }

    public record AvailabilityItemRequest(
            @NotBlank(message = "Day of week is required.")
            String dayOfWeek,

            boolean isAvailable,

            @NotNull(message = "Start time is required.")
            LocalTime startTime,

            @NotNull(message = "End time is required.")
            LocalTime endTime,

            LocalTime breakStartTime,
            LocalTime breakEndTime,

            Integer slotDurationMinutes,
            Integer maxPatients,

            String consultationMode
    ) {
    }
}