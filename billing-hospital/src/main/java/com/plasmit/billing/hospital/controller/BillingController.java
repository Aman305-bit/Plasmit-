package com.plasmit.billing.hospital.controller;

import java.util.List;
import java.util.Map;

import com.plasmit.billing.hospital.common.ApiResponse;
import com.plasmit.billing.hospital.repository.BillingRepository.BillingServiceRecord;
import com.plasmit.billing.hospital.repository.BillingRepository.DashboardBootstrap;
import com.plasmit.billing.hospital.repository.BillingRepository.InvoiceRecord;
import com.plasmit.billing.hospital.service.BillingService;
import com.plasmit.billing.hospital.service.BillingService.BillingPreviewResponse;
import com.plasmit.billing.hospital.service.BillingService.CancelRequest;
import com.plasmit.billing.hospital.service.BillingService.InvoiceDetailResponse;
import com.plasmit.billing.hospital.service.BillingService.InvoiceRequest;
import com.plasmit.billing.hospital.service.BillingService.PaymentRequest;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hospital")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final BillingService service;

    public BillingController(BillingService service) {
        this.service = service;
    }

    @GetMapping("/billing-desk/bootstrap")
    public ResponseEntity<ApiResponse<DashboardBootstrap>> bootstrap() {

        log.info("Billing desk bootstrap API called.");

        DashboardBootstrap response = service.bootstrap();

        log.info("Billing desk bootstrap API completed.");

        return ResponseEntity.ok(ApiResponse.success("Billing desk bootstrap fetched successfully.", response));
    }

    @GetMapping("/billing/services")
    public ResponseEntity<ApiResponse<List<BillingServiceRecord>>> billingServices(
            @RequestParam(required = false) String query
    ) {

        log.info("Billing services API called. query={}", query);

        List<BillingServiceRecord> response = service.billingServices(query);

        log.info("Billing services API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Billing services fetched successfully.", response));
    }

    @PostMapping("/billing/preview")
    public ResponseEntity<ApiResponse<BillingPreviewResponse>> preview(
            @Valid @RequestBody InvoiceRequest request
    ) {

        log.info("Billing preview API called.");

        BillingPreviewResponse response = service.previewInvoice(request);

        log.info("Billing preview API completed. itemCount={}", response.items().size());

        return ResponseEntity.ok(ApiResponse.success("Billing preview generated successfully.", response));
    }

    @PostMapping("/billing/invoices")
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> createInvoice(
            @Valid @RequestBody InvoiceRequest request
    ) {

        log.info("Create billing invoice API called.");

        InvoiceDetailResponse response = service.createInvoice(request);

        log.info("Create billing invoice API completed. invoiceId={}", response.invoice().id());

        return ResponseEntity.ok(ApiResponse.success("Billing invoice created successfully.", response));
    }

    @GetMapping("/billing/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceRecord>>> invoices(
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String invoiceStatus,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        log.info("Billing invoice list API called.");

        List<InvoiceRecord> response = service.listInvoices(paymentStatus, invoiceStatus, patientId, page, size);

        log.info("Billing invoice list API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Billing invoices fetched successfully.", response));
    }

    @GetMapping("/billing/invoices/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> invoiceDetail(
            @PathVariable Long invoiceId
    ) {

        log.info("Billing invoice detail API called. invoiceId={}", invoiceId);

        InvoiceDetailResponse response = service.getInvoice(invoiceId);

        log.info("Billing invoice detail API completed. invoiceId={}", invoiceId);

        return ResponseEntity.ok(ApiResponse.success("Billing invoice fetched successfully.", response));
    }

    @PostMapping("/billing/invoices/{invoiceId}/payments")
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> addPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody PaymentRequest request
    ) {

        log.info("Billing payment API called. invoiceId={}", invoiceId);

        InvoiceDetailResponse response = service.addPayment(invoiceId, request);

        log.info("Billing payment API completed. invoiceId={}", invoiceId);

        return ResponseEntity.ok(ApiResponse.success("Payment added successfully.", response));
    }

    @PostMapping("/billing/invoices/{invoiceId}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelInvoice(
            @PathVariable Long invoiceId,
            @RequestBody(required = false) CancelRequest request
    ) {

        log.info("Billing invoice cancel API called. invoiceId={}", invoiceId);

        Map<String, Object> response = service.cancelInvoice(invoiceId, request);

        log.info("Billing invoice cancel API completed. invoiceId={}", invoiceId);

        return ResponseEntity.ok(ApiResponse.success("Billing invoice cancelled successfully.", response));
    }
}