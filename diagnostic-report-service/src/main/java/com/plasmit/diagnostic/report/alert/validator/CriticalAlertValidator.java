package com.plasmit.diagnostic.report.alert.validator;

import com.plasmit.diagnostic.report.alert.dto.request.*;
import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.validator.DateRangeValidator;
import com.plasmit.diagnostic.report.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class CriticalAlertValidator {

    private static final Set<String> STATUSES = Set.of(
            "Open",
            "Acknowledged",
            "Escalated",
            "Closed"
    );

    private static final Set<String> SEVERITIES = Set.of(
            "Critical",
            "High",
            "Urgent"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public CriticalAlertValidator(DateRangeValidator dateRangeValidator,
                                  PaginationValidator paginationValidator) {
        this.dateRangeValidator = dateRangeValidator;
        this.paginationValidator = paginationValidator;
    }

    public void validateList(LocalDate fromDate,
                             LocalDate toDate,
                             Integer page,
                             Integer limit,
                             String status,
                             String severity) {

        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
        paginationValidator.validate(page, limit);

        if (status != null && !status.isBlank() && !STATUSES.contains(status)) {
            throw ApiException.validation("Invalid alert status.");
        }

        if (severity != null && !severity.isBlank() && !SEVERITIES.contains(severity)) {
            throw ApiException.validation("Invalid severity.");
        }
    }

    public void validateReportId(Long reportId) {
        if (reportId == null || reportId <= 0) {
            throw ApiException.validation("Valid reportId is required.");
        }
    }

    public void validateAlertId(Long alertId) {
        if (alertId == null || alertId <= 0) {
            throw ApiException.validation("Valid alertId is required.");
        }
    }

    public void validateCreate(CreateCriticalAlertRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.alertMessage() == null || request.alertMessage().isBlank()) {
            throw ApiException.validation("alertMessage is required.");
        }

        if (request.severity() != null
                && !request.severity().isBlank()
                && !SEVERITIES.contains(request.severity())) {
            throw ApiException.validation("Invalid severity.");
        }
    }

    public void validateAcknowledge(AcknowledgeCriticalAlertRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.acknowledgedByName() == null || request.acknowledgedByName().isBlank()) {
            throw ApiException.validation("acknowledgedByName is required.");
        }
    }

    public void validateEscalate(EscalateCriticalAlertRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.escalatedToUserId() == null || request.escalatedToUserId() <= 0) {
            throw ApiException.validation("Valid escalatedToUserId is required.");
        }

        if (request.escalatedToName() == null || request.escalatedToName().isBlank()) {
            throw ApiException.validation("escalatedToName is required.");
        }

        if (request.escalationReason() == null || request.escalationReason().isBlank()) {
            throw ApiException.validation("escalationReason is required.");
        }
    }

    public void validateClose(CloseCriticalAlertRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.closeNotes() == null || request.closeNotes().isBlank()) {
            throw ApiException.validation("closeNotes is required.");
        }
    }
}