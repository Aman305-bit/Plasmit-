package com.plasmit.diagnostics.payment.diagnostics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasmit.diagnostics.payment.common.exception.ApiException;
import com.plasmit.diagnostics.payment.common.response.PageResponse;
import com.plasmit.diagnostics.payment.common.response.PaginationMeta;
import com.plasmit.diagnostics.payment.context.TenantContext;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.BillingAuthorizationRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.DiagnosticOrderCreateRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.DiagnosticOrderStatusUpdateRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.BillingAuthorizationResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.DiagnosticOrderResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.DiagnosticsBootstrapResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.DiagnosticsSummaryResponse;
import com.plasmit.diagnostics.payment.diagnostics.repository.DiagnosticOrderRepository;
import com.plasmit.diagnostics.payment.diagnostics.validator.DiagnosticOrderValidator;
import com.plasmit.diagnostics.payment.validator.CommonRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DiagnosticOrderService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticOrderService.class);

    private final DiagnosticOrderRepository repository;
    private final CommonRequestValidator commonRequestValidator;
    private final DiagnosticOrderValidator validator;
    private final ObjectMapper objectMapper;

    public DiagnosticOrderService(DiagnosticOrderRepository repository,
                                  CommonRequestValidator commonRequestValidator,
                                  DiagnosticOrderValidator validator,
                                  ObjectMapper objectMapper) {
        this.repository = repository;
        this.commonRequestValidator = commonRequestValidator;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public DiagnosticsBootstrapResponse getBootstrap() {
        validateContext();

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        log.info("Fetching diagnostics bootstrap. tenantId={} hospitalId={} branchId={}",
                tenantId, hospitalId, branchId);

        return new DiagnosticsBootstrapResponse(
                repository.findBranches(tenantId, hospitalId),
                repository.findDepartments(tenantId, hospitalId, branchId),
                repository.findModalities(tenantId, hospitalId, branchId),
                validator.allowedPriorities(),
                validator.allowedStatuses(),
                validator.allowedSources()
        );
    }

    public DiagnosticsSummaryResponse getSummary() {
        validateContext();

        return repository.getSummary(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId()
        );
    }

    public PageResponse<DiagnosticOrderResponse> getOrders(LocalDate fromDate,
                                                          LocalDate toDate,
                                                          String department,
                                                          String status,
                                                          String priority,
                                                          String source,
                                                          String search,
                                                          Integer page,
                                                          Integer limit) {
        validateContext();
        validator.validateListRequest(fromDate, toDate, page, limit);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        Long total = repository.countOrders(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                department,
                status,
                priority,
                source,
                search
        );

        List<DiagnosticOrderResponse> rows = repository.findOrders(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                department,
                status,
                priority,
                source,
                search,
                page,
                limit
        );

        return new PageResponse<>(
                rows,
                new PaginationMeta(page, limit, total)
        );
    }

    @Transactional
    public DiagnosticOrderResponse createOrder(DiagnosticOrderCreateRequest request,
                                               String idempotencyKey) {
        validateContext();
        commonRequestValidator.validateIdempotencyKey(idempotencyKey);
        validator.validateCreateRequest(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        Long duplicateCount = repository.countDuplicatePending(
                tenantId,
                hospitalId,
                branchId,
                request.patientId(),
                request.serviceIds()
        );

        if (duplicateCount > 0) {
            throw ApiException.duplicate("Duplicate pending diagnostic order already exists.");
        }

        List<Map<String, Object>> services = repository.findServicesForOrder(
                tenantId,
                hospitalId,
                branchId,
                request.serviceIds()
        );

        if (services.size() != request.serviceIds().size()) {
            throw ApiException.validation("One or more diagnostic services are invalid.");
        }

        String orderNo = generateOrderNo();
        String accessionNo = null;
        String barcodeNo = null;

        if ("Radiology".equalsIgnoreCase(request.department())) {
            accessionNo = "ACC-" + System.currentTimeMillis();
        }

        if ("Pathology".equalsIgnoreCase(request.department())) {
            barcodeNo = "BC-" + System.currentTimeMillis();
        }

        LocalDateTime dueAt = calculateDueAt(request.priority(), request.preferredAt());

        String billingAuthStatus = request.invoiceId() == null ? "Pending" : "Approved";
        boolean releaseBlocked = request.invoiceId() == null;

        log.info("Creating diagnostic order. tenantId={} hospitalId={} branchId={} orderNo={}",
                tenantId, hospitalId, branchId, orderNo);

        Long orderId = repository.createOrder(
                tenantId,
                hospitalId,
                branchId,
                userId,
                request,
                orderNo,
                toJson(request.diagnosisCodes() == null ? Collections.emptyList() : request.diagnosisCodes()),
                toJson(request.flags() == null ? Collections.emptyList() : request.flags()),
                accessionNo,
                barcodeNo,
                dueAt,
                billingAuthStatus,
                releaseBlocked
        );

        for (Map<String, Object> service : services) {
            repository.createOrderLine(
                    tenantId,
                    hospitalId,
                    branchId,
                    orderId,
                    service
            );
        }

        repository.insertStatusHistory(
                tenantId,
                hospitalId,
                branchId,
                orderId,
                null,
                "Ordered",
                "Order created.",
                request.clinicalNotes(),
                userId,
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        repository.insertDomainEvent(
                tenantId,
                hospitalId,
                branchId,
                "diagnostics.order.created",
                "DiagnosticOrder",
                String.valueOf(orderId),
                userId,
                TenantContext.getRequestId(),
                toJson(Map.of("orderNo", orderNo, "status", "Ordered"))
        );

        return repository.findOrderById(tenantId, hospitalId, branchId, orderId);
    }

    @Transactional
    public DiagnosticOrderResponse updateStatus(Long orderId,
                                                DiagnosticOrderStatusUpdateRequest request) {
        validateContext();
        validator.validateOrderId(orderId);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        DiagnosticOrderResponse existing = repository.findOrderById(
                tenantId,
                hospitalId,
                branchId,
                orderId
        );

        validator.validateStatusUpdate(request, existing.status());

        repository.updateOrderStatus(
                tenantId,
                hospitalId,
                branchId,
                orderId,
                request.status(),
                userId
        );

        repository.insertStatusHistory(
                tenantId,
                hospitalId,
                branchId,
                orderId,
                existing.status(),
                request.status(),
                request.reason(),
                request.notes(),
                userId,
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        repository.insertDomainEvent(
                tenantId,
                hospitalId,
                branchId,
                "diagnostics.order.status_changed",
                "DiagnosticOrder",
                String.valueOf(orderId),
                userId,
                TenantContext.getRequestId(),
                toJson(Map.of(
                        "fromStatus", existing.status(),
                        "toStatus", request.status()
                ))
        );

        return repository.findOrderById(tenantId, hospitalId, branchId, orderId);
    }

    @Transactional
    public BillingAuthorizationResponse authorizeBilling(Long orderId,
                                                        BillingAuthorizationRequest request) {
        validateContext();
        validator.validateOrderId(orderId);
        validator.validateBillingAuthorization(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        repository.findOrderById(tenantId, hospitalId, branchId, orderId);

        BillingAuthorizationResponse response = repository.createBillingAuthorization(
                tenantId,
                hospitalId,
                branchId,
                orderId,
                userId,
                TenantContext.getRole(),
                request
        );

        repository.insertDomainEvent(
                tenantId,
                hospitalId,
                branchId,
                "diagnostics.billing_authorization.updated",
                "DiagnosticOrder",
                String.valueOf(orderId),
                userId,
                TenantContext.getRequestId(),
                toJson(Map.of(
                        "action", request.action(),
                        "authorizationStatus", response.authorizationStatus(),
                        "releaseBlocked", response.releaseBlocked()
                ))
        );

        return response;
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }

    private LocalDateTime calculateDueAt(String priority, LocalDateTime preferredAt) {
        LocalDateTime base = preferredAt == null ? LocalDateTime.now() : preferredAt;

        if ("STAT".equalsIgnoreCase(priority)) {
            return base.plusMinutes(60);
        }

        if ("Urgent".equalsIgnoreCase(priority)) {
            return base.plusHours(4);
        }

        return base.plusHours(24);
    }

    private String generateOrderNo() {
        return "DORD-" + System.currentTimeMillis();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw ApiException.validation("Invalid JSON payload.");
        }
    }
}