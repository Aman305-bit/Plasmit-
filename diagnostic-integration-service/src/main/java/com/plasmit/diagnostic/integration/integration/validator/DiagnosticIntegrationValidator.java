package com.plasmit.diagnostic.integration.integration.validator;

import com.plasmit.diagnostic.integration.common.exception.ApiException;
import com.plasmit.diagnostic.integration.integration.dto.request.*;
import com.plasmit.diagnostic.integration.validator.DateRangeValidator;
import com.plasmit.diagnostic.integration.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class DiagnosticIntegrationValidator {

    private static final Set<String> EVENT_STATUSES = Set.of(
            "Received", "Processed", "Failed", "RetryScheduled", "Retried"
    );

    private static final Set<String> DIRECTIONS = Set.of(
            "INBOUND", "OUTBOUND", "BIDIRECTIONAL"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public DiagnosticIntegrationValidator(DateRangeValidator dateRangeValidator,
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

        if (status != null && !status.isBlank() && !EVENT_STATUSES.contains(status)) {
            throw ApiException.validation("Invalid exchange status.");
        }
    }

    public void validateNodeId(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw ApiException.validation("Valid nodeId is required.");
        }
    }

    public void validateEventId(Long eventId) {
        if (eventId == null || eventId <= 0) {
            throw ApiException.validation("Valid eventId is required.");
        }
    }

    public void validateCreateEvent(CreateExchangeEventRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        validateNodeId(request.nodeId());

        if (request.eventType() == null || request.eventType().isBlank()) {
            throw ApiException.validation("eventType is required.");
        }

        if (request.direction() == null || request.direction().isBlank()) {
            throw ApiException.validation("direction is required.");
        }

        if (!DIRECTIONS.contains(request.direction())) {
            throw ApiException.validation("Invalid direction.");
        }

        if ((request.rawPayload() == null || request.rawPayload().isBlank())
                && (request.parsedPayload() == null || request.parsedPayload().isBlank())) {
            throw ApiException.validation("rawPayload or parsedPayload is required.");
        }
    }

    public void validateFail(FailExchangeEventRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.errorMessage() == null || request.errorMessage().isBlank()) {
            throw ApiException.validation("errorMessage is required.");
        }
    }

    public void validateRetry(RetryExchangeEventRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }
    }
}