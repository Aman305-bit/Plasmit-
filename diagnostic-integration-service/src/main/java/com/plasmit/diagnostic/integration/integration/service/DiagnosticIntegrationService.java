package com.plasmit.diagnostic.integration.integration.service;

import com.plasmit.diagnostic.integration.common.exception.ApiException;
import com.plasmit.diagnostic.integration.common.response.PageResponse;
import com.plasmit.diagnostic.integration.common.response.PaginationMeta;
import com.plasmit.diagnostic.integration.context.TenantContext;
import com.plasmit.diagnostic.integration.integration.dto.request.*;
import com.plasmit.diagnostic.integration.integration.dto.response.*;
import com.plasmit.diagnostic.integration.integration.repository.DiagnosticIntegrationRepository;
import com.plasmit.diagnostic.integration.integration.validator.DiagnosticIntegrationValidator;
import com.plasmit.diagnostic.integration.validator.CommonRequestValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DiagnosticIntegrationService {

    private final DiagnosticIntegrationRepository repository;
    private final DiagnosticIntegrationValidator validator;
    private final CommonRequestValidator commonRequestValidator;

    public DiagnosticIntegrationService(DiagnosticIntegrationRepository repository,
                                        DiagnosticIntegrationValidator validator,
                                        CommonRequestValidator commonRequestValidator) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
    }

    public List<IntegrationNodeResponse> getNodes(String integrationType, String status) {
        validateContext();

        return repository.findNodes(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                integrationType,
                status
        );
    }

    public IntegrationNodeResponse getNode(Long nodeId) {
        validateContext();
        validator.validateNodeId(nodeId);

        return repository.findNodeById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                nodeId
        );
    }

    @Transactional
    public IntegrationNodeResponse heartbeat(Long nodeId, HeartbeatRequest request) {
        validateContext();
        validator.validateNodeId(nodeId);

        IntegrationNodeResponse before = getNode(nodeId);

        repository.updateHeartbeat(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                nodeId,
                TenantContext.getUserId()
        );

        audit("INTEGRATION_NODE", nodeId, "NODE_HEARTBEAT",
                before.nodeStatus(), "Active",
                request == null ? "Heartbeat received." : request.statusMessage());

        return getNode(nodeId);
    }

    @Transactional
    public ExchangeEventResponse createEvent(CreateExchangeEventRequest request) {
        validateContext();
        validator.validateCreateEvent(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        String eventCode = "INT-" + System.currentTimeMillis();

        Long eventId = repository.createExchangeEvent(
                tenantId,
                hospitalId,
                branchId,
                TenantContext.getUserId(),
                request,
                eventCode
        );

        if (request.rawPayload() != null && !request.rawPayload().isBlank()) {
            repository.createPayload(
                    tenantId,
                    hospitalId,
                    branchId,
                    eventId,
                    "RAW",
                    request.contentType(),
                    request.rawPayload(),
                    null
            );
        }

        if (request.parsedPayload() != null && !request.parsedPayload().isBlank()) {
            repository.createPayload(
                    tenantId,
                    hospitalId,
                    branchId,
                    eventId,
                    "PARSED",
                    request.contentType(),
                    null,
                    request.parsedPayload()
            );
        }

        audit("EXCHANGE_EVENT", eventId, "EXCHANGE_EVENT_RECEIVED",
                null, "Received",
                "Integration exchange event received.");

        return repository.findEventById(tenantId, hospitalId, branchId, eventId);
    }

    public PageResponse<ExchangeEventResponse> getEvents(LocalDate fromDate,
                                                         LocalDate toDate,
                                                         String status,
                                                         String eventType,
                                                         Long nodeId,
                                                         String search,
                                                         Integer page,
                                                         Integer limit) {

        validateContext();
        validator.validateList(fromDate, toDate, page, limit, status);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        Long total = repository.countEvents(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                eventType,
                nodeId,
                search
        );

        List<ExchangeEventResponse> rows = repository.findEvents(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                eventType,
                nodeId,
                search,
                page,
                limit
        );

        return new PageResponse<>(
                rows,
                new PaginationMeta(page, limit, total)
        );
    }

    public ExchangeEventResponse getEvent(Long eventId) {
        validateContext();
        validator.validateEventId(eventId);

        return repository.findEventById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                eventId
        );
    }

    @Transactional
    public ExchangeEventResponse processEvent(Long eventId, ProcessExchangeEventRequest request) {
        validateContext();
        validator.validateEventId(eventId);

        ExchangeEventResponse before = getEvent(eventId);

        if ("Processed".equals(before.exchangeStatus())) {
            throw ApiException.workflow("Exchange event is already processed.");
        }

        repository.updateEventStatus(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                eventId,
                "Processed",
                null,
                TenantContext.getUserId()
        );

        audit("EXCHANGE_EVENT", eventId, "EXCHANGE_EVENT_PROCESSED",
                before.exchangeStatus(), "Processed",
                request == null ? "Event processed." : request.notes());

        return getEvent(eventId);
    }

    @Transactional
    public ExchangeEventResponse failEvent(Long eventId, FailExchangeEventRequest request) {
        validateContext();
        validator.validateEventId(eventId);
        validator.validateFail(request);

        ExchangeEventResponse before = getEvent(eventId);

        if ("Processed".equals(before.exchangeStatus())) {
            throw ApiException.workflow("Processed exchange event cannot be marked failed.");
        }

        repository.updateEventStatus(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                eventId,
                "Failed",
                request.errorMessage(),
                TenantContext.getUserId()
        );

        audit("EXCHANGE_EVENT", eventId, "EXCHANGE_EVENT_FAILED",
                before.exchangeStatus(), "Failed",
                request.errorMessage());

        return getEvent(eventId);
    }

    @Transactional
    public RetryLogResponse retryEvent(Long eventId, RetryExchangeEventRequest request) {
        validateContext();
        validator.validateEventId(eventId);
        validator.validateRetry(request);

        ExchangeEventResponse event = getEvent(eventId);

        if (!"Failed".equals(event.exchangeStatus())
                && !"RetryScheduled".equals(event.exchangeStatus())) {
            throw ApiException.workflow("Only Failed or RetryScheduled event can be retried.");
        }

        RetryLogResponse response = repository.createRetryLog(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                event,
                TenantContext.getUserId(),
                request
        );

        audit("EXCHANGE_EVENT", eventId, "EXCHANGE_EVENT_RETRY_SCHEDULED",
                event.exchangeStatus(), "RetryScheduled",
                request.retryReason());

        return response;
    }

    public List<RetryLogResponse> getRetryLogs(Long eventId) {
        validateContext();
        validator.validateEventId(eventId);

        getEvent(eventId);

        return repository.findRetryLogs(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                eventId
        );
    }

    public List<IntegrationAuditResponse> getEventAudit(Long eventId) {
        validateContext();
        validator.validateEventId(eventId);

        getEvent(eventId);

        return repository.findAudit(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                "EXCHANGE_EVENT",
                eventId
        );
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }

    private void audit(String entityType,
                       Long entityId,
                       String eventType,
                       String fromStatus,
                       String toStatus,
                       String notes) {

        repository.insertAudit(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                entityType,
                entityId,
                eventType,
                fromStatus,
                toStatus,
                notes,
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );
    }
}