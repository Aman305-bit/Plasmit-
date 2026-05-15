package com.plasmit.diagnostics.payment.diagnostics.validator;

import com.plasmit.diagnostics.payment.common.exception.ApiException;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.BillingAuthorizationRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.DiagnosticOrderCreateRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.DiagnosticOrderStatusUpdateRequest;
import com.plasmit.diagnostics.payment.validator.DateRangeValidator;
import com.plasmit.diagnostics.payment.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
public class DiagnosticOrderValidator {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "Draft",
            "Ordered",
            "AwaitingPayment",
            "PaidApproved",
            "Scheduled",
            "PatientPending",
            "SamplePending",
            "Collected",
            "InProcess",
            "ResultEntered",
            "TechnicalVerified",
            "ClinicalVerified",
            "Signed",
            "Released",
            "Delivered",
            "Cancelled",
            "Rejected",
            "Amended"
    );

    private static final Set<String> TERMINAL_STATUSES = Set.of(
            "Released",
            "Delivered",
            "Cancelled",
            "Rejected"
    );

    private static final Set<String> ALLOWED_PRIORITIES = Set.of(
            "Routine",
            "Urgent",
            "STAT"
    );

    private static final Set<String> ALLOWED_SOURCES = Set.of(
            "OPD",
            "IPD",
            "Emergency",
            "BillingDesk",
            "Package",
            "Direct"
    );

    private static final Set<String> ALLOWED_BILLING_ACTIONS = Set.of(
            "Approve",
            "Block",
            "Override"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public DiagnosticOrderValidator(DateRangeValidator dateRangeValidator,
                                    PaginationValidator paginationValidator) {
        this.dateRangeValidator = dateRangeValidator;
        this.paginationValidator = paginationValidator;
    }

    public void validateListRequest(LocalDate fromDate,
                                    LocalDate toDate,
                                    Integer page,
                                    Integer limit) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
        paginationValidator.validate(page, limit);
    }

    public void validateCreateRequest(DiagnosticOrderCreateRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.patientId() == null || request.patientId() <= 0) {
            throw ApiException.validation("Valid patientId is required.");
        }

        if (request.patientName() == null || request.patientName().isBlank()) {
            throw ApiException.validation("patientName is required.");
        }

        if (request.source() == null || request.source().isBlank()) {
            throw ApiException.validation("source is required.");
        }

        if (!ALLOWED_SOURCES.contains(request.source())) {
            throw ApiException.validation("Invalid source.");
        }

        if (request.department() == null || request.department().isBlank()) {
            throw ApiException.validation("department is required.");
        }

        if (request.serviceIds() == null || request.serviceIds().isEmpty()) {
            throw ApiException.validation("At least one serviceId is required.");
        }

        for (Long serviceId : request.serviceIds()) {
            if (serviceId == null || serviceId <= 0) {
                throw ApiException.validation("Valid serviceId is required.");
            }
        }

        if (request.priority() == null || request.priority().isBlank()) {
            throw ApiException.validation("priority is required.");
        }

        if (!ALLOWED_PRIORITIES.contains(request.priority())) {
            throw ApiException.validation("Invalid priority.");
        }

        if (request.clinicalNotes() != null && request.clinicalNotes().length() > 1000) {
            throw ApiException.validation("clinicalNotes cannot exceed 1000 characters.");
        }
    }

    public void validateStatusUpdate(DiagnosticOrderStatusUpdateRequest request,
                                     String currentStatus) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.status() == null || request.status().isBlank()) {
            throw ApiException.validation("status is required.");
        }

        if (!ALLOWED_STATUSES.contains(request.status())) {
            throw ApiException.validation("Invalid diagnostic order status.");
        }

        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw ApiException.workflow("Terminal order cannot be updated.");
        }

        if ("Cancelled".equals(request.status())
                && (request.reason() == null || request.reason().isBlank())) {
            throw ApiException.validation("reason is required for cancellation.");
        }
    }

    public void validateBillingAuthorization(BillingAuthorizationRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.action() == null || request.action().isBlank()) {
            throw ApiException.validation("action is required.");
        }

        if (!ALLOWED_BILLING_ACTIONS.contains(request.action())) {
            throw ApiException.validation("Invalid billing authorization action.");
        }

        if (("Block".equals(request.action()) || "Override".equals(request.action()))
                && (request.reason() == null || request.reason().isBlank())) {
            throw ApiException.validation("reason is required for block/override action.");
        }
    }

    public void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw ApiException.validation("Valid orderId is required.");
        }
    }

    public List<String> allowedStatuses() {
        return ALLOWED_STATUSES.stream().sorted().toList();
    }

    public List<String> allowedPriorities() {
        return ALLOWED_PRIORITIES.stream().sorted().toList();
    }

    public List<String> allowedSources() {
        return ALLOWED_SOURCES.stream().sorted().toList();
    }
}