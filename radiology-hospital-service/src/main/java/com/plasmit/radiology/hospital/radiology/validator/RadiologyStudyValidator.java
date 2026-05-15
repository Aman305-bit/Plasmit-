package com.plasmit.radiology.hospital.radiology.validator;

import com.plasmit.radiology.hospital.common.exception.ApiException;
import com.plasmit.radiology.hospital.radiology.dto.request.PacsMatchRequest;
import com.plasmit.radiology.hospital.radiology.dto.request.RadiologyScheduleRequest;
import com.plasmit.radiology.hospital.radiology.dto.request.SafetyChecklistRequest;
import com.plasmit.radiology.hospital.validator.DateRangeValidator;
import com.plasmit.radiology.hospital.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class RadiologyStudyValidator {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "Ordered",
            "Scheduled",
            "ReadyForScan",
            "InProgress",
            "Completed",
            "Cancelled"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public RadiologyStudyValidator(DateRangeValidator dateRangeValidator,
                                   PaginationValidator paginationValidator) {
        this.dateRangeValidator = dateRangeValidator;
        this.paginationValidator = paginationValidator;
    }

    public void validateStudyList(LocalDate fromDate, LocalDate toDate, Integer page, Integer limit) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
        paginationValidator.validate(page, limit);
    }

    public void validateStudyId(Long studyId) {
        if (studyId == null || studyId <= 0) {
            throw ApiException.validation("Valid studyId is required.");
        }
    }

    public void validateSchedule(RadiologyScheduleRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.scheduledAt() == null) {
            throw ApiException.validation("scheduledAt is required.");
        }

        if (request.scheduledRoom() == null || request.scheduledRoom().isBlank()) {
            throw ApiException.validation("scheduledRoom is required.");
        }
    }

    public void validateSafetyChecklist(SafetyChecklistRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.consentTaken() == null || !request.consentTaken()) {
            throw ApiException.validation("Consent must be taken before proceeding.");
        }

        if (request.remarks() == null || request.remarks().isBlank()) {
            throw ApiException.validation("remarks is required.");
        }
    }

    public void validatePacsMatch(PacsMatchRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.pacsStudyUid() == null || request.pacsStudyUid().isBlank()) {
            throw ApiException.validation("pacsStudyUid is required.");
        }

        if (request.matchConfidence() != null
                && (request.matchConfidence() < 0 || request.matchConfidence() > 100)) {
            throw ApiException.validation("matchConfidence must be between 0 and 100.");
        }
    }

    public void validateStatus(String status) {
        if (status != null && !status.isBlank() && !ALLOWED_STATUSES.contains(status)) {
            throw ApiException.validation("Invalid radiology study status.");
        }
    }
}