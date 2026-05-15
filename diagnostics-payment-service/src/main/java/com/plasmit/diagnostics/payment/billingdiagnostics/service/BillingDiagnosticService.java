package com.plasmit.diagnostics.payment.billingdiagnostics.service;

import com.plasmit.diagnostics.payment.billingdiagnostics.dto.request.*;
import com.plasmit.diagnostics.payment.billingdiagnostics.dto.response.*;
import com.plasmit.diagnostics.payment.billingdiagnostics.repository.BillingDiagnosticRepository;
import com.plasmit.diagnostics.payment.billingdiagnostics.validator.BillingDiagnosticValidator;
import com.plasmit.diagnostics.payment.common.exception.ApiException;
import com.plasmit.diagnostics.payment.context.TenantContext;
import com.plasmit.diagnostics.payment.validator.CommonRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingDiagnosticService {

    private static final Logger log = LoggerFactory.getLogger(BillingDiagnosticService.class);

    private final BillingDiagnosticRepository repository;
    private final CommonRequestValidator commonRequestValidator;
    private final BillingDiagnosticValidator validator;

    public BillingDiagnosticService(BillingDiagnosticRepository repository,
                                    CommonRequestValidator commonRequestValidator,
                                    BillingDiagnosticValidator validator) {
        this.repository = repository;
        this.commonRequestValidator = commonRequestValidator;
        this.validator = validator;
    }

    public BillingDiagnosticBootstrapResponse getBootstrap() {
        validateContext();

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        return new BillingDiagnosticBootstrapResponse(
                repository.findDepartments(tenantId, hospitalId, branchId),
                repository.findModalities(tenantId, hospitalId, branchId),
                repository.findPayerTypes(tenantId, hospitalId, branchId)
        );
    }

    public List<DiagnosticServiceResponse> searchServices(String payerType,
                                                          String department,
                                                          String modality,
                                                          String search,
                                                          Integer page,
                                                          Integer limit) {
        validateContext();
        validator.validateServiceSearch(page, limit);

        if (payerType == null || payerType.isBlank()) {
            payerType = "SelfPay";
        }

        return repository.searchServices(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                payerType,
                department,
                modality,
                search,
                page,
                limit
        );
    }

    public DiagnosticServiceResponse getService(Long serviceId, String payerType) {
        validateContext();

        if (serviceId == null || serviceId <= 0) {
            throw ApiException.validation("Valid serviceId is required.");
        }

        if (payerType == null || payerType.isBlank()) {
            payerType = "SelfPay";
        }

        return repository.findServiceById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                serviceId,
                payerType
        );
    }

    public DiagnosticCartValidateResponse validateCart(DiagnosticCartValidateRequest request) {
        validateContext();
        validator.validateCart(request);

        return calculateCart(request.payerType(), request.items());
    }

    @Transactional
    public DiagnosticInvoiceResponse createInvoice(DiagnosticInvoiceCreateRequest request,
                                                   String idempotencyKey) {
        validateContext();
        commonRequestValidator.validateIdempotencyKey(idempotencyKey);
        validator.validateInvoiceCreate(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        DiagnosticCartValidateResponse cart = calculateCart(request.payerType(), request.items());

        long paidPaise = request.payments() == null
                ? 0L
                : request.payments().stream().mapToLong(DiagnosticPaymentRequest::amountPaise).sum();

        long duePaise = cart.netPaise() - paidPaise;

        if (paidPaise > cart.netPaise()) {
            throw ApiException.validation("Paid amount cannot be greater than net amount.");
        }

        String status = paidPaise == 0 ? "Due" : duePaise == 0 ? "Paid" : "Partial";
        String paymentMethod = request.payments() == null || request.payments().isEmpty()
                ? "Credit"
                : request.payments().get(0).paymentMethod();

        String invoiceNo = generateInvoiceNo();

        log.info("Creating diagnostic invoice. tenantId={} hospitalId={} branchId={} invoiceNo={}",
                tenantId, hospitalId, branchId, invoiceNo);

        Long invoiceId = repository.createInvoice(
                tenantId,
                hospitalId,
                branchId,
                userId,
                request,
                invoiceNo,
                cart.grossPaise(),
                cart.discountPaise(),
                cart.taxPaise(),
                cart.netPaise(),
                paidPaise,
                duePaise,
                paymentMethod,
                status
        );

        if (request.payments() != null) {
            for (DiagnosticPaymentRequest payment : request.payments()) {
                if (payment.amountPaise() != null && payment.amountPaise() > 0) {
                    repository.createPaymentTransaction(
                            tenantId,
                            hospitalId,
                            branchId,
                            invoiceId,
                            payment.paymentMethod(),
                            payment.amountPaise(),
                            payment.referenceNo(),
                            userId
                    );
                }
            }
        }

        List<Long> handoffIds = new ArrayList<>();

        for (DiagnosticCartLineResponse line : cart.lines()) {
            DiagnosticServiceResponse service = repository.findServiceById(
                    tenantId,
                    hospitalId,
                    branchId,
                    line.serviceId(),
                    request.payerType()
            );

            Long invoiceLineId = repository.createInvoiceLine(
                    tenantId,
                    hospitalId,
                    branchId,
                    invoiceId,
                    service,
                    line.quantity(),
                    line.grossPaise(),
                    line.discountPaise(),
                    line.taxPaise(),
                    line.netPaise()
            );

            String route = "Radiology".equalsIgnoreCase(service.department()) ? "RIS" : "LIS";

            Long handoffId = repository.createHandoff(
                    tenantId,
                    hospitalId,
                    branchId,
                    invoiceId,
                    invoiceLineId,
                    service.serviceId(),
                    service.modality(),
                    route,
                    userId
            );

            handoffIds.add(handoffId);
        }

        return new DiagnosticInvoiceResponse(
                invoiceId,
                invoiceNo,
                status,
                cart.grossPaise(),
                cart.discountPaise(),
                cart.taxPaise(),
                cart.netPaise(),
                paidPaise,
                duePaise,
                handoffIds
        );
    }

    public List<DiagnosticHandoffResponse> getHandoffs(String status) {
        validateContext();

        return repository.findHandoffs(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                status
        );
    }

    @Transactional
    public String retryHandoff(Long handoffId) {
        validateContext();
        validator.validateHandoffId(handoffId);

        repository.retryHandoff(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                handoffId
        );

        return "Diagnostic handoff retry queued successfully.";
    }

    @Transactional
    public String cancelHandoff(Long handoffId, HandoffCancelRequest request) {
        validateContext();
        validator.validateHandoffId(handoffId);

        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw ApiException.validation("reason is required.");
        }

        repository.cancelHandoff(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                handoffId,
                request.reason()
        );

        return "Diagnostic handoff cancelled successfully.";
    }

    private DiagnosticCartValidateResponse calculateCart(String payerType, List<DiagnosticCartItemRequest> items) {
        List<Long> serviceIds = items.stream().map(DiagnosticCartItemRequest::serviceId).toList();

        List<DiagnosticServiceResponse> services = repository.searchServicesByIds(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                serviceIds,
                payerType
        );

        if (services.size() != serviceIds.size()) {
            throw ApiException.validation("One or more diagnostic services are invalid or tariff is missing.");
        }

        List<DiagnosticCartLineResponse> lines = new ArrayList<>();

        long grossTotal = 0;
        long discountTotal = 0;
        long taxTotal = 0;
        long netTotal = 0;

        for (DiagnosticCartItemRequest item : items) {
            DiagnosticServiceResponse service = services.stream()
                    .filter(s -> s.serviceId().equals(item.serviceId()))
                    .findFirst()
                    .orElseThrow(() -> ApiException.validation("Service tariff missing."));

            long gross = service.pricePaise() * item.quantity();
            long discount = item.discountPaise() == null ? 0L : item.discountPaise();

            if (discount > gross) {
                throw ApiException.validation("Discount cannot be greater than gross amount.");
            }

            long taxable = gross - discount;

            long tax = BigDecimal.valueOf(taxable)
                    .multiply(BigDecimal.valueOf(service.taxPercent()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValue();

            long net = taxable + tax;

            lines.add(new DiagnosticCartLineResponse(
                    service.serviceId(),
                    service.serviceCode(),
                    service.serviceName(),
                    service.department(),
                    service.modality(),
                    item.quantity(),
                    service.pricePaise(),
                    gross,
                    discount,
                    tax,
                    net
            ));

            grossTotal += gross;
            discountTotal += discount;
            taxTotal += tax;
            netTotal += net;
        }

        return new DiagnosticCartValidateResponse(
                lines,
                grossTotal,
                discountTotal,
                taxTotal,
                netTotal
        );
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }

    private String generateInvoiceNo() {
        return "DINV-" + System.currentTimeMillis();
    }
}