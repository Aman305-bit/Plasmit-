package com.plasmit.diagnostics.payment.paymentreports.controller;

import com.plasmit.diagnostics.payment.common.response.ApiResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentBootstrapResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentSummaryResponse;
import com.plasmit.diagnostics.payment.paymentreports.service.PaymentReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import com.plasmit.diagnostics.payment.common.response.PageResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentLedgerRowResponse;
import java.time.LocalDate;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.CollectionMixResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.DepartmentBillingResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.DueAgingResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.SettlementExceptionResponse;
import com.plasmit.diagnostics.payment.common.constant.HeaderConstants;
import com.plasmit.diagnostics.payment.paymentreports.dto.request.ReconciliationRunRequest;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.ReconciliationRunResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.request.ExportJobRequest;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.ExportJobResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentAuditResponse;
import com.plasmit.diagnostics.payment.common.response.PageResponse;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital/payment-reports")
@CrossOrigin("*")
public class PaymentReportController {

    private static final Logger log = LoggerFactory.getLogger(PaymentReportController.class);

    private final PaymentReportService paymentReportService;

    public PaymentReportController(PaymentReportService paymentReportService) {
        this.paymentReportService = paymentReportService;
    }

    @GetMapping("/summary")
    public ApiResponse<PaymentSummaryResponse> getSummary(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {

        log.info("Payment report summary API called. fromDate={} toDate={}", fromDate, toDate);

        PaymentSummaryResponse response = paymentReportService.getSummary(fromDate, toDate);

        return ApiResponse.success(
                "Payment report summary fetched successfully.",
                response
        );
    }

    @GetMapping("/bootstrap")
    public ApiResponse<PaymentBootstrapResponse> getBootstrap(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {

        log.info("Payment report bootstrap API called. fromDate={} toDate={}", fromDate, toDate);

        PaymentBootstrapResponse response = paymentReportService.getBootstrap(fromDate, toDate);

        return ApiResponse.success(
                "Payment report bootstrap fetched successfully.",
                response
        );
    }
    @GetMapping("/ledger")
    public ApiResponse<PageResponse<PaymentLedgerRowResponse>> getLedger(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "department", required = false)
            String department,

            @RequestParam(value = "payerType", required = false)
            String payerType,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "paymentMethod", required = false)
            String paymentMethod,

            @RequestParam(value = "settlementStatus", required = false)
            String settlementStatus,

            @RequestParam(value = "cashierId", required = false)
            Long cashierId,

            @RequestParam(value = "search", required = false)
            String search,

            @RequestParam(value = "page", defaultValue = "1")
            Integer page,

            @RequestParam(value = "limit", defaultValue = "25")
            Integer limit,

            @RequestParam(value = "sortBy", defaultValue = "billDateTime")
            String sortBy,

            @RequestParam(value = "sortOrder", defaultValue = "desc")
            String sortOrder
    ) {

        log.info("Payment report ledger API called. fromDate={} toDate={} page={} limit={}",
                fromDate, toDate, page, limit);

        PageResponse<PaymentLedgerRowResponse> response = paymentReportService.getLedger(
                fromDate,
                toDate,
                department,
                payerType,
                status,
                paymentMethod,
                settlementStatus,
                cashierId,
                search,
                page,
                limit,
                sortBy,
                sortOrder
        );

        return ApiResponse.success(
                "Payment report ledger fetched successfully.",
                response
        );
    }
    @GetMapping("/collection-mix")
    public ApiResponse<List<CollectionMixResponse>> getCollectionMix(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "department", required = false)
            String department,

            @RequestParam(value = "payerType", required = false)
            String payerType,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "cashierId", required = false)
            Long cashierId
    ) {

        log.info("Payment report collection mix API called. fromDate={} toDate={}", fromDate, toDate);

        List<CollectionMixResponse> response = paymentReportService.getCollectionMix(
                fromDate,
                toDate,
                department,
                payerType,
                status,
                cashierId
        );

        return ApiResponse.success(
                "Payment report collection mix fetched successfully.",
                response
        );
    }
    @GetMapping("/department-billing")
    public ApiResponse<List<DepartmentBillingResponse>> getDepartmentBilling(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "payerType", required = false)
            String payerType,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "paymentMethod", required = false)
            String paymentMethod,

            @RequestParam(value = "cashierId", required = false)
            Long cashierId
    ) {

        log.info("Payment report department billing API called. fromDate={} toDate={}", fromDate, toDate);

        List<DepartmentBillingResponse> response = paymentReportService.getDepartmentBilling(
                fromDate,
                toDate,
                payerType,
                status,
                paymentMethod,
                cashierId
        );

        return ApiResponse.success(
                "Payment report department billing fetched successfully.",
                response
        );
    }
    @GetMapping("/due-aging")
    public ApiResponse<List<DueAgingResponse>> getDueAging(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "department", required = false)
            String department,

            @RequestParam(value = "payerType", required = false)
            String payerType,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "paymentMethod", required = false)
            String paymentMethod,

            @RequestParam(value = "cashierId", required = false)
            Long cashierId
    ) {

        log.info("Payment report due aging API called. fromDate={} toDate={}", fromDate, toDate);

        List<DueAgingResponse> response = paymentReportService.getDueAging(
                fromDate,
                toDate,
                department,
                payerType,
                status,
                paymentMethod,
                cashierId
        );

        return ApiResponse.success(
                "Payment report due aging fetched successfully.",
                response
        );
    }
    @GetMapping("/settlement-exceptions")
    public ApiResponse<List<SettlementExceptionResponse>> getSettlementExceptions(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "paymentMethod", required = false)
            String paymentMethod,

            @RequestParam(value = "severity", required = false)
            String severity,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "owner", required = false)
            String owner
    ) {

        log.info("Payment report settlement exceptions API called. fromDate={} toDate={}", fromDate, toDate);

        List<SettlementExceptionResponse> response = paymentReportService.getSettlementExceptions(
                fromDate,
                toDate,
                paymentMethod,
                severity,
                status,
                owner
        );

        return ApiResponse.success(
                "Payment report settlement exceptions fetched successfully.",
                response
        );
    }
    @PostMapping("/reconciliation-runs")
    public ApiResponse<ReconciliationRunResponse> createReconciliationRun(
            @RequestHeader(value = HeaderConstants.X_IDEMPOTENCY_KEY, required = false)
            String idempotencyKey,

            @Valid @RequestBody
            ReconciliationRunRequest request
    ) {

        log.info("Create reconciliation run API called. fromDate={} toDate={}",
                request.getFromDate(), request.getToDate());

        ReconciliationRunResponse response = paymentReportService.createReconciliationRun(
                request,
                idempotencyKey
        );

        return ApiResponse.success(
                "Reconciliation run queued successfully.",
                response
        );
    }
    @GetMapping("/reconciliation-runs/{runId}")
    public ApiResponse<ReconciliationRunResponse> getReconciliationRun(
            @PathVariable("runId") Long runId
    ) {

        log.info("Get reconciliation run API called. runId={}", runId);

        ReconciliationRunResponse response = paymentReportService.getReconciliationRun(runId);

        return ApiResponse.success(
                "Reconciliation run fetched successfully.",
                response
        );
    }
    @PostMapping("/exports")
    public ApiResponse<ExportJobResponse> createExportJob(
            @RequestHeader(value = HeaderConstants.X_IDEMPOTENCY_KEY, required = false)
            String idempotencyKey,

            @Valid @RequestBody
            ExportJobRequest request
    ) {

        log.info("Create export job API called. reportType={} format={}",
                request.getReportType(), request.getFormat());

        ExportJobResponse response = paymentReportService.createExportJob(
                request,
                idempotencyKey
        );

        return ApiResponse.success(
                "Export job queued successfully.",
                response
        );
    }
    @GetMapping("/exports/{exportId}")
    public ApiResponse<ExportJobResponse> getExportJob(
            @PathVariable("exportId") Long exportId
    ) {

        log.info("Get export job API called. exportId={}", exportId);

        ExportJobResponse response = paymentReportService.getExportJob(exportId);

        return ApiResponse.success(
                "Export job fetched successfully.",
                response
        );
    }
    @GetMapping("/audit")
    public ApiResponse<PageResponse<PaymentAuditResponse>> getAudit(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "entityType", required = false)
            String entityType,

            @RequestParam(value = "action", required = false)
            String action,

            @RequestParam(value = "actorUserId", required = false)
            Long actorUserId,

            @RequestParam(value = "page", defaultValue = "1")
            Integer page,

            @RequestParam(value = "limit", defaultValue = "25")
            Integer limit
    ) {

        log.info("Payment report audit API called. fromDate={} toDate={} page={} limit={}",
                fromDate, toDate, page, limit);

        PageResponse<PaymentAuditResponse> response = paymentReportService.getAudit(
                fromDate,
                toDate,
                entityType,
                action,
                actorUserId,
                page,
                limit
        );

        return ApiResponse.success(
                "Payment report audit fetched successfully.",
                response
        );
    }
}