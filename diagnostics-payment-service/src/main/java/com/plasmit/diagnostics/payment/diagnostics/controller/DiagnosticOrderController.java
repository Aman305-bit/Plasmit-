package com.plasmit.diagnostics.payment.diagnostics.controller;

import com.plasmit.diagnostics.payment.common.constant.HeaderConstants;
import com.plasmit.diagnostics.payment.common.response.ApiResponse;
import com.plasmit.diagnostics.payment.common.response.PageResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.BillingAuthorizationRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.DiagnosticOrderCreateRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.DiagnosticOrderStatusUpdateRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.BillingAuthorizationResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.DiagnosticOrderResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.DiagnosticsBootstrapResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.DiagnosticsSummaryResponse;
import com.plasmit.diagnostics.payment.diagnostics.service.DiagnosticOrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/hospital/diagnostics")
@CrossOrigin("*")
public class DiagnosticOrderController {

    private final DiagnosticOrderService service;

    public DiagnosticOrderController(DiagnosticOrderService service) {
        this.service = service;
    }

    @GetMapping("/bootstrap")
    public ApiResponse<DiagnosticsBootstrapResponse> getBootstrap() {
        return ApiResponse.success(
                "Diagnostics bootstrap fetched successfully.",
                service.getBootstrap()
        );
    }

    @GetMapping("/summary")
    public ApiResponse<DiagnosticsSummaryResponse> getSummary() {
        return ApiResponse.success(
                "Diagnostics summary fetched successfully.",
                service.getSummary()
        );
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<DiagnosticOrderResponse>> getOrders(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "department", required = false)
            String department,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "priority", required = false)
            String priority,

            @RequestParam(value = "source", required = false)
            String source,

            @RequestParam(value = "search", required = false)
            String search,

            @RequestParam(value = "page", defaultValue = "1")
            Integer page,

            @RequestParam(value = "limit", defaultValue = "25")
            Integer limit
    ) {
        PageResponse<DiagnosticOrderResponse> response = service.getOrders(
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

        return ApiResponse.success(
                "Diagnostic orders fetched successfully.",
                response
        );
    }

    @PostMapping("/orders")
    public ApiResponse<DiagnosticOrderResponse> createOrder(
            @RequestHeader(value = HeaderConstants.X_IDEMPOTENCY_KEY, required = false)
            String idempotencyKey,

            @Valid @RequestBody
            DiagnosticOrderCreateRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic order created successfully.",
                service.createOrder(request, idempotencyKey)
        );
    }

    @PatchMapping("/orders/{orderId}/status")
    public ApiResponse<DiagnosticOrderResponse> updateStatus(
            @PathVariable Long orderId,

            @Valid @RequestBody
            DiagnosticOrderStatusUpdateRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic order status updated successfully.",
                service.updateStatus(orderId, request)
        );
    }

    @PostMapping("/orders/{orderId}/billing-authorization")
    public ApiResponse<BillingAuthorizationResponse> authorizeBilling(
            @PathVariable Long orderId,

            @Valid @RequestBody
            BillingAuthorizationRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic billing authorization updated successfully.",
                service.authorizeBilling(orderId, request)
        );
    }
}