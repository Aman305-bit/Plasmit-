package com.plasmit.pathology.hospital.pathology.validator;

import com.plasmit.pathology.hospital.common.exception.ApiException;
import com.plasmit.pathology.hospital.pathology.dto.request.*;
import com.plasmit.pathology.hospital.validator.DateRangeValidator;
import com.plasmit.pathology.hospital.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class PathologySpecimenValidator {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "SamplePending",
            "Collected",
            "Received",
            "Rejected",
            "InProcess",
            "ResultEntered",
            "TechnicalVerified",
            "ClinicalVerified",
            "Released"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public PathologySpecimenValidator(DateRangeValidator dateRangeValidator,
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

        if (status != null && !status.isBlank() && !ALLOWED_STATUSES.contains(status)) {
            throw ApiException.validation("Invalid specimen status.");
        }
    }

    public void validateSpecimenId(Long specimenId) {
        if (specimenId == null || specimenId <= 0) {
            throw ApiException.validation("Valid specimenId is required.");
        }
    }

    public void validateCollect(CollectSpecimenRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.collectedByName() == null || request.collectedByName().isBlank()) {
            throw ApiException.validation("collectedByName is required.");
        }
    }

    public void validateReceive(ReceiveSpecimenRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.receivedByName() == null || request.receivedByName().isBlank()) {
            throw ApiException.validation("receivedByName is required.");
        }
    }

    public void validateReject(RejectSpecimenRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw ApiException.validation("reason is required.");
        }
    }

    public void validateResultEntry(ResultEntryRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.parameters() == null || request.parameters().isEmpty()) {
            throw ApiException.validation("At least one result parameter is required.");
        }

        for (ResultParameterRequest parameter : request.parameters()) {
            if (parameter.parameterName() == null || parameter.parameterName().isBlank()) {
                throw ApiException.validation("parameterName is required.");
            }
        }
    }

    public void validateVerification(VerificationRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.verifierName() == null || request.verifierName().isBlank()) {
            throw ApiException.validation("verifierName is required.");
        }
    }
}