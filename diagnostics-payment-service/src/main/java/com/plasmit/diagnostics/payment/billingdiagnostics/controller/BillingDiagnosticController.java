package com.plasmit.diagnostics.payment.billingdiagnostics.controller;

import com.plasmit.diagnostics.payment.billingdiagnostics.dto.request.DiagnosticCartValidateRequest;
import com.plasmit.diagnostics.payment.billingdiagnostics.dto.request.DiagnosticInvoiceCreateRequest;
import com.plasmit.diagnostics.payment.billingdiagnostics.dto.request.HandoffCancelRequest;
import com.plasmit.diagnostics.payment.billingdiagnostics.dto.response.*;
import com.plasmit.diagnostics.payment.billingdiagnostics.service.BillingDiagnosticService;
import com.plasmit.diagnostics.payment.common.constant.HeaderConstants;
import com.plasmit.diagnostics.payment.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital/billing-desk")
@CrossOrigin("*")
public class BillingDiagnosticController {

    private final BillingDiagnosticService service;

    public BillingDiagnosticController(BillingDiagnosticService service) {
        this.service = service;
    }

    @GetMapping("/diagnostics/bootstrap")
    public ApiResponse<BillingDiagnosticBootstrapResponse> getBootstrap() {
        return ApiResponse.success(
                "Billing diagnostics bootstrap fetched successfully.",
                service.getBootstrap()
        );
    }

    @GetMapping("/diagnostic-services")
    public ApiResponse<List<DiagnosticServiceResponse>> searchServices(
            @RequestParam(value = "payerType", defaultValue = "SelfPay") String payerType,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "modality", required = false) String modality,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "limit", defaultValue = "25") Integer limit
    ) {
        return ApiResponse.success(
                "Diagnostic services fetched successfully.",
                service.searchServices(payerType, department, modality, search, page, limit)
        );
    }

    @GetMapping("/diagnostic-services/{serviceId}")
    public ApiResponse<DiagnosticServiceResponse> getService(
            @PathVariable Long serviceId,
            @RequestParam(value = "payerType", defaultValue = "SelfPay") String payerType
    ) {
        return ApiResponse.success(
                "Diagnostic service fetched successfully.",
                service.getService(serviceId, payerType)
        );
    }

    @PostMapping("/diagnostic-cart/validate")
    public ApiResponse<DiagnosticCartValidateResponse> validateCart(
            @Valid @RequestBody DiagnosticCartValidateRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic cart validated successfully.",
                service.validateCart(request)
        );
    }

    @PostMapping("/diagnostic-invoices")
    public ApiResponse<DiagnosticInvoiceResponse> createInvoice(
            @RequestHeader(value = HeaderConstants.X_IDEMPOTENCY_KEY, required = false)
            String idempotencyKey,

            @Valid @RequestBody
            DiagnosticInvoiceCreateRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic invoice created successfully.",
                service.createInvoice(request, idempotencyKey)
        );
    }

    @GetMapping("/diagnostic-handoffs")
    public ApiResponse<List<DiagnosticHandoffResponse>> getHandoffs(
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(
                "Diagnostic handoffs fetched successfully.",
                service.getHandoffs(status)
        );
    }

    @PostMapping("/diagnostic-handoffs/{handoffId}/retry")
    public ApiResponse<String> retryHandoff(@PathVariable Long handoffId) {
        return ApiResponse.success(
                "Diagnostic handoff retry queued successfully.",
                service.retryHandoff(handoffId)
        );
    }

    @PostMapping("/diagnostic-handoffs/{handoffId}/cancel")
    public ApiResponse<String> cancelHandoff(
            @PathVariable Long handoffId,
            @Valid @RequestBody HandoffCancelRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic handoff cancelled successfully.",
                service.cancelHandoff(handoffId, request)
        );
    }
}