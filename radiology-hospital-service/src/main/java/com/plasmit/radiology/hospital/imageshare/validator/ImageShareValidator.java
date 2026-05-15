package com.plasmit.radiology.hospital.imageshare.validator;

import com.plasmit.radiology.hospital.common.exception.ApiException;
import com.plasmit.radiology.hospital.imageshare.dto.request.CreateImageShareRequest;
import com.plasmit.radiology.hospital.imageshare.dto.request.RevokeImageShareRequest;
import com.plasmit.radiology.hospital.validator.DateRangeValidator;
import com.plasmit.radiology.hospital.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Component
public class ImageShareValidator {

    private static final Set<String> ALLOWED_STATUS = Set.of(
            "Active",
            "Revoked",
            "Expired"
    );

    private static final Set<String> ALLOWED_ACCESS_SCOPE = Set.of(
            "VIEW_ONLY",
            "VIEW_DOWNLOAD",
            "SECOND_OPINION"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public ImageShareValidator(DateRangeValidator dateRangeValidator,
                               PaginationValidator paginationValidator) {
        this.dateRangeValidator = dateRangeValidator;
        this.paginationValidator = paginationValidator;
    }

    public void validateList(LocalDate fromDate,
                             LocalDate toDate,
                             Integer page,
                             Integer limit,
                             String status) {

        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
        paginationValidator.validate(page, limit);

        if (status != null && !status.isBlank() && !ALLOWED_STATUS.contains(status)) {
            throw ApiException.validation("Invalid share status.");
        }
    }

    public void validateCreate(CreateImageShareRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.studyId() == null || request.studyId() <= 0) {
            throw ApiException.validation("Valid studyId is required.");
        }

        if (request.sharePurpose() == null || request.sharePurpose().isBlank()) {
            throw ApiException.validation("sharePurpose is required.");
        }

        if (request.accessScope() == null || request.accessScope().isBlank()) {
            throw ApiException.validation("accessScope is required.");
        }

        if (!ALLOWED_ACCESS_SCOPE.contains(request.accessScope())) {
            throw ApiException.validation("Invalid accessScope.");
        }

        if (request.expiresAt() == null) {
            throw ApiException.validation("expiresAt is required.");
        }

        if (!request.expiresAt().isAfter(LocalDateTime.now())) {
            throw ApiException.validation("expiresAt must be in future.");
        }

        if ((request.recipientMobile() == null || request.recipientMobile().isBlank())
                && (request.recipientEmail() == null || request.recipientEmail().isBlank())) {
            throw ApiException.validation("recipientMobile or recipientEmail is required.");
        }
    }

    public void validateShareId(Long shareId) {
        if (shareId == null || shareId <= 0) {
            throw ApiException.validation("Valid shareId is required.");
        }
    }

    public void validateRevoke(RevokeImageShareRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw ApiException.validation("reason is required.");
        }
    }
}