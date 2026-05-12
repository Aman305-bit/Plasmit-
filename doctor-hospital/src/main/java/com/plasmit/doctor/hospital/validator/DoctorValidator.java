package com.plasmit.doctor.hospital.validator;

import java.time.LocalTime;
import java.util.List;

import com.plasmit.doctor.hospital.exception.ApiException;
import com.plasmit.doctor.hospital.repository.DoctorRepository.AvailabilityRecord;
import com.plasmit.doctor.hospital.repository.DoctorRepository.DoctorRecord;
import com.plasmit.doctor.hospital.service.DoctorService.AvailabilityItemRequest;
import com.plasmit.doctor.hospital.service.DoctorService.CreateDoctorRequest;
import com.plasmit.doctor.hospital.service.DoctorService.UpdateAvailabilityRequest;
import com.plasmit.doctor.hospital.service.DoctorService.UpdateDoctorRequest;
import com.plasmit.doctor.hospital.service.DoctorService.UpdateFeeRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DoctorValidator {

    private static final Logger log = LoggerFactory.getLogger(DoctorValidator.class);

    public void validateTenantHospitalContext(Long tenantId, Long hospitalId) {
        if (tenantId == null || hospitalId == null) {
            log.warn("Doctor validation failed. reason=TENANT_OR_HOSPITAL_MISSING tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (tenantId <= 0 || hospitalId <= 0) {
            log.warn("Doctor validation failed. reason=INVALID_TENANT_OR_HOSPITAL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateBranchContext(Long branchId) {
        if (branchId != null && branchId <= 0) {
            log.warn("Doctor validation failed. reason=INVALID_BRANCH_ID branchId={}", branchId);
            throw ApiException.badRequest("Invalid branch id.");
        }
    }

    public int validatePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public int validateSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }

        if (size > 100) {
            log.warn("Doctor validation warning. reason=PAGE_SIZE_LIMIT_EXCEEDED requestedSize={}", size);
            return 100;
        }

        return size;
    }

    public void validateStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return;
        }

        if (!status.equalsIgnoreCase("ACTIVE")
                && !status.equalsIgnoreCase("INACTIVE")
                && !status.equalsIgnoreCase("ARCHIVED")) {
            log.warn("Doctor validation failed. reason=INVALID_STATUS status={}", status);
            throw ApiException.badRequest("Status must be ACTIVE, INACTIVE or ARCHIVED.");
        }
    }

    public void validateDoctorId(Long doctorId) {
        if (doctorId == null || doctorId <= 0) {
            log.warn("Doctor validation failed. reason=INVALID_DOCTOR_ID doctorId={}", doctorId);
            throw ApiException.badRequest("Invalid doctor id.");
        }
    }

    public void validateDoctorFound(DoctorRecord doctor, Long doctorId, Long tenantId, Long hospitalId) {
        if (doctor == null) {
            log.warn("Doctor validation failed. reason=DOCTOR_NOT_FOUND doctorId={} tenantId={} hospitalId={}",
                    doctorId, tenantId, hospitalId);
            throw ApiException.notFound("Doctor not found.");
        }
    }

    public void validateDoctorList(List<DoctorRecord> doctors, Long tenantId, Long hospitalId) {
        if (doctors == null) {
            log.warn("Doctor validation failed. reason=DOCTOR_LIST_NULL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.badRequest("Unable to fetch doctors.");
        }

        for (DoctorRecord doctor : doctors) {
            if (doctor.id() == null || doctor.id() <= 0) {
                log.warn("Doctor validation failed. reason=INVALID_DOCTOR_RECORD tenantId={} hospitalId={} doctor={}",
                        tenantId, hospitalId, doctor);
                throw ApiException.badRequest("Invalid doctor data.");
            }
        }
    }

    public void validateSearchQuery(String query) {
        if (query == null || query.isBlank()) {
            log.warn("Doctor validation failed. reason=SEARCH_QUERY_REQUIRED");
            throw ApiException.badRequest("Search query is required.");
        }

        if (query.trim().length() < 2) {
            log.warn("Doctor validation failed. reason=SEARCH_QUERY_TOO_SHORT query={}", query);
            throw ApiException.badRequest("Search query must be at least 2 characters.");
        }
    }

    public void validateCreateRequest(CreateDoctorRequest request) {
        if (request == null) {
            log.warn("Doctor validation failed. reason=CREATE_REQUEST_NULL");
            throw ApiException.badRequest("Doctor request is required.");
        }

        validateGender(request.gender());
        validatePhone(request.phone());
        validateExperienceYears(request.experienceYears());
        validateConsultationDuration(request.consultationDurationMinutes());
    }

    public void validateUpdateRequest(UpdateDoctorRequest request) {
        if (request == null) {
            log.warn("Doctor validation failed. reason=UPDATE_REQUEST_NULL");
            throw ApiException.badRequest("Doctor request is required.");
        }

        validateGender(request.gender());
        validatePhone(request.phone());
        validateExperienceYears(request.experienceYears());
        validateConsultationDuration(request.consultationDurationMinutes());
    }

    public void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            log.warn("Doctor validation failed. reason=PHONE_REQUIRED");
            throw ApiException.badRequest("Phone is required.");
        }

        if (!phone.matches("^[0-9]{10,15}$")) {
            log.warn("Doctor validation failed. reason=INVALID_PHONE phone={}", phone);
            throw ApiException.badRequest("Phone must be 10 to 15 digits.");
        }
    }

    public void validateGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return;
        }

        if (!gender.equalsIgnoreCase("MALE")
                && !gender.equalsIgnoreCase("FEMALE")
                && !gender.equalsIgnoreCase("OTHER")) {
            log.warn("Doctor validation failed. reason=INVALID_GENDER gender={}", gender);
            throw ApiException.badRequest("Gender must be MALE, FEMALE or OTHER.");
        }
    }

    public void validateExperienceYears(Integer experienceYears) {
        if (experienceYears != null && (experienceYears < 0 || experienceYears > 80)) {
            log.warn("Doctor validation failed. reason=INVALID_EXPERIENCE_YEARS experienceYears={}", experienceYears);
            throw ApiException.badRequest("Experience years must be between 0 and 80.");
        }
    }

    public void validateConsultationDuration(Integer minutes) {
        if (minutes != null && (minutes < 5 || minutes > 240)) {
            log.warn("Doctor validation failed. reason=INVALID_CONSULTATION_DURATION minutes={}", minutes);
            throw ApiException.badRequest("Consultation duration must be between 5 and 240 minutes.");
        }
    }

    public void validateDuplicatePhone(boolean exists, String phone, Long tenantId, Long hospitalId) {
        if (exists) {
            log.warn("Doctor validation failed. reason=DUPLICATE_PHONE tenantId={} hospitalId={} phone={}",
                    tenantId, hospitalId, phone);
            throw ApiException.conflict("Doctor already exists with this phone number.");
        }
    }

    public void validateDuplicateRegistration(boolean exists, String registrationNumber, Long tenantId, Long hospitalId) {
        if (exists) {
            log.warn("Doctor validation failed. reason=DUPLICATE_REGISTRATION tenantId={} hospitalId={} registration={}",
                    tenantId, hospitalId, registrationNumber);
            throw ApiException.conflict("Doctor already exists with this registration number.");
        }
    }

    public void validateDuplicatePhoneForUpdate(boolean exists, String phone, Long doctorId, Long tenantId, Long hospitalId) {
        if (exists) {
            log.warn("Doctor validation failed. reason=DUPLICATE_PHONE_FOR_UPDATE doctorId={} tenantId={} hospitalId={} phone={}",
                    doctorId, tenantId, hospitalId, phone);
            throw ApiException.conflict("Another doctor already exists with this phone number.");
        }
    }

    public void validateDuplicateRegistrationForUpdate(boolean exists, String registrationNumber, Long doctorId, Long tenantId, Long hospitalId) {
        if (exists) {
            log.warn("Doctor validation failed. reason=DUPLICATE_REGISTRATION_FOR_UPDATE doctorId={} tenantId={} hospitalId={} registration={}",
                    doctorId, tenantId, hospitalId, registrationNumber);
            throw ApiException.conflict("Another doctor already exists with this registration number.");
        }
    }

    public void validateUpdateCount(int updated, Long doctorId, Long tenantId, Long hospitalId) {
        if (updated == 0) {
            log.warn("Doctor validation failed. reason=DOCTOR_UPDATE_FAILED_OR_NOT_FOUND doctorId={} tenantId={} hospitalId={}",
                    doctorId, tenantId, hospitalId);
            throw ApiException.notFound("Doctor not found.");
        }
    }

    public void validateDeleteCount(int updated, Long doctorId, Long tenantId, Long hospitalId) {
        if (updated == 0) {
            log.warn("Doctor validation failed. reason=DOCTOR_DELETE_FAILED_OR_NOT_FOUND doctorId={} tenantId={} hospitalId={}",
                    doctorId, tenantId, hospitalId);
            throw ApiException.notFound("Doctor not found.");
        }
    }

    public void validateAvailabilityList(List<AvailabilityRecord> availability, Long doctorId, Long tenantId, Long hospitalId) {
        if (availability == null) {
            log.warn("Doctor validation failed. reason=AVAILABILITY_LIST_NULL doctorId={} tenantId={} hospitalId={}",
                    doctorId, tenantId, hospitalId);
            throw ApiException.badRequest("Unable to fetch doctor availability.");
        }
    }

    public void validateAvailabilityRequest(UpdateAvailabilityRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            log.warn("Doctor validation failed. reason=AVAILABILITY_ITEMS_REQUIRED");
            throw ApiException.badRequest("Availability items are required.");
        }

        for (AvailabilityItemRequest item : request.items()) {
            validateAvailabilityItem(item);
        }
    }

    public void validateAvailabilityItem(AvailabilityItemRequest item) {
        if (item == null) {
            log.warn("Doctor validation failed. reason=AVAILABILITY_ITEM_NULL");
            throw ApiException.badRequest("Availability item is required.");
        }

        if (item.dayOfWeek() == null || item.dayOfWeek().isBlank()) {
            log.warn("Doctor validation failed. reason=DAY_OF_WEEK_REQUIRED");
            throw ApiException.badRequest("Day of week is required.");
        }

        String day = item.dayOfWeek().trim().toUpperCase();
        if (!List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY").contains(day)) {
            log.warn("Doctor validation failed. reason=INVALID_DAY_OF_WEEK dayOfWeek={}", item.dayOfWeek());
            throw ApiException.badRequest("Invalid day of week.");
        }

        if (!item.isAvailable()) {
            return;
        }

        validateTimeRange(item.startTime(), item.endTime(), "Availability time range");

        if (item.breakStartTime() != null || item.breakEndTime() != null) {
            validateTimeRange(item.breakStartTime(), item.breakEndTime(), "Break time range");
        }

        if (item.slotDurationMinutes() != null && (item.slotDurationMinutes() < 5 || item.slotDurationMinutes() > 240)) {
            log.warn("Doctor validation failed. reason=INVALID_SLOT_DURATION slotDuration={}", item.slotDurationMinutes());
            throw ApiException.badRequest("Slot duration must be between 5 and 240 minutes.");
        }

        if (item.maxPatients() != null && item.maxPatients() < 0) {
            log.warn("Doctor validation failed. reason=INVALID_MAX_PATIENTS maxPatients={}", item.maxPatients());
            throw ApiException.badRequest("Max patients cannot be negative.");
        }
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime, String label) {
        if (startTime == null || endTime == null) {
            log.warn("Doctor validation failed. reason=TIME_RANGE_REQUIRED label={}", label);
            throw ApiException.badRequest(label + " is required.");
        }

        if (!endTime.isAfter(startTime)) {
            log.warn("Doctor validation failed. reason=INVALID_TIME_RANGE label={} start={} end={}",
                    label, startTime, endTime);
            throw ApiException.badRequest(label + " end time must be after start time.");
        }
    }

    public void validateFeeRequest(UpdateFeeRequest request) {
        if (request == null) {
            log.warn("Doctor validation failed. reason=FEE_REQUEST_NULL");
            throw ApiException.badRequest("Fee request is required.");
        }
    }
}