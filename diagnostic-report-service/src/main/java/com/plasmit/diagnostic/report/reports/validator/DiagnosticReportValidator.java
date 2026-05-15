package com.plasmit.diagnostic.report.reports.validator;

import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.reports.dto.request.*;
import com.plasmit.diagnostic.report.validator.DateRangeValidator;
import com.plasmit.diagnostic.report.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class DiagnosticReportValidator {

    private static final Set<String> SOURCE_TYPES = Set.of("PATHOLOGY", "RADIOLOGY");

    private static final Set<String> STATUSES = Set.of(
            "Draft",
            "ExpertReviewPending",
            "ExpertReviewed",
            "Signed",
            "Released",
            "Amended"
    );

    private static final Set<String> REVIEW_STATUSES = Set.of(
            "Approved",
            "ChangesRequired",
            "Rejected"
    );

    private static final Set<String> RELEASE_CHANNELS = Set.of(
            "Portal",
            "Email",
            "Print",
            "WhatsApp"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public DiagnosticReportValidator(DateRangeValidator dateRangeValidator,
                                     PaginationValidator paginationValidator) {
        this.dateRangeValidator = dateRangeValidator;
        this.paginationValidator = paginationValidator;
    }

    public void validateList(LocalDate fromDate,
                             LocalDate toDate,
                             Integer page,
                             Integer limit,
                             String status,
                             String sourceType) {

        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
        paginationValidator.validate(page, limit);

        if (status != null && !status.isBlank() && !STATUSES.contains(status)) {
            throw ApiException.validation("Invalid report status.");
        }

        if (sourceType != null && !sourceType.isBlank() && !SOURCE_TYPES.contains(sourceType)) {
            throw ApiException.validation("Invalid sourceType.");
        }
    }

    public void validateCreate(CreateDiagnosticReportRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.sourceType() == null || request.sourceType().isBlank()) {
            throw ApiException.validation("sourceType is required.");
        }

        if (!SOURCE_TYPES.contains(request.sourceType())) {
            throw ApiException.validation("sourceType must be PATHOLOGY or RADIOLOGY.");
        }

        if (request.sourceId() == null || request.sourceId() <= 0) {
            throw ApiException.validation("Valid sourceId is required.");
        }

        if (request.patientId() == null || request.patientId() <= 0) {
            throw ApiException.validation("Valid patientId is required.");
        }

        if (request.patientName() == null || request.patientName().isBlank()) {
            throw ApiException.validation("patientName is required.");
        }

        if (request.department() == null || request.department().isBlank()) {
            throw ApiException.validation("department is required.");
        }
    }

    public void validateReportId(Long reportId) {
        if (reportId == null || reportId <= 0) {
            throw ApiException.validation("Valid reportId is required.");
        }
    }

    public void validateExpertReview(ExpertReviewRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.reviewerName() == null || request.reviewerName().isBlank()) {
            throw ApiException.validation("reviewerName is required.");
        }

        if (request.reviewStatus() == null || request.reviewStatus().isBlank()) {
            throw ApiException.validation("reviewStatus is required.");
        }

        if (!REVIEW_STATUSES.contains(request.reviewStatus())) {
            throw ApiException.validation("Invalid reviewStatus.");
        }
    }

    public void validateSign(SignReportRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.signerName() == null || request.signerName().isBlank()) {
            throw ApiException.validation("signerName is required.");
        }
    }

    public void validateRelease(ReleaseReportRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.releaseChannel() == null || request.releaseChannel().isBlank()) {
            throw ApiException.validation("releaseChannel is required.");
        }

        if (!RELEASE_CHANNELS.contains(request.releaseChannel())) {
            throw ApiException.validation("Invalid releaseChannel.");
        }
    }

    public void validateAmend(AmendReportRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.amendmentReason() == null || request.amendmentReason().isBlank()) {
            throw ApiException.validation("amendmentReason is required.");
        }

        if (request.amendedContent() == null || request.amendedContent().isBlank()) {
            throw ApiException.validation("amendedContent is required.");
        }
    }
}