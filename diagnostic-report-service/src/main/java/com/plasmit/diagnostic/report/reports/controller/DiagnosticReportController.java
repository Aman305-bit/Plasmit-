package com.plasmit.diagnostic.report.reports.controller;

import com.plasmit.diagnostic.report.common.response.ApiResponse;
import com.plasmit.diagnostic.report.common.response.PageResponse;
import com.plasmit.diagnostic.report.reports.dto.request.*;
import com.plasmit.diagnostic.report.reports.dto.response.*;
import com.plasmit.diagnostic.report.reports.service.DiagnosticReportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital/diagnostic-reports")
@CrossOrigin("*")
public class DiagnosticReportController {

    private final DiagnosticReportService service;

    public DiagnosticReportController(DiagnosticReportService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<DiagnosticReportResponse>> getReports(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "sourceType", required = false)
            String sourceType,

            @RequestParam(value = "department", required = false)
            String department,

            @RequestParam(value = "search", required = false)
            String search,

            @RequestParam(value = "page", defaultValue = "1")
            Integer page,

            @RequestParam(value = "limit", defaultValue = "25")
            Integer limit
    ) {
        return ApiResponse.success(
                "Diagnostic reports fetched successfully.",
                service.getReports(fromDate, toDate, status, sourceType, department, search, page, limit)
        );
    }

    @GetMapping("/{reportId}")
    public ApiResponse<DiagnosticReportResponse> getReport(@PathVariable Long reportId) {
        return ApiResponse.success(
                "Diagnostic report fetched successfully.",
                service.getReport(reportId)
        );
    }

    @PostMapping
    public ApiResponse<DiagnosticReportResponse> createReport(
            @Valid @RequestBody CreateDiagnosticReportRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic report created successfully.",
                service.createReport(request)
        );
    }

    @PostMapping("/{reportId}/expert-review")
    public ApiResponse<ExpertReviewResponse> expertReview(
            @PathVariable Long reportId,
            @Valid @RequestBody ExpertReviewRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic report expert review saved successfully.",
                service.expertReview(reportId, request)
        );
    }

    @PostMapping("/{reportId}/sign")
    public ApiResponse<ReportSignatureResponse> sign(
            @PathVariable Long reportId,
            @Valid @RequestBody SignReportRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic report signed successfully.",
                service.signReport(reportId, request)
        );
    }

    @PostMapping("/{reportId}/release")
    public ApiResponse<ReportReleaseResponse> release(
            @PathVariable Long reportId,
            @Valid @RequestBody ReleaseReportRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic report released successfully.",
                service.releaseReport(reportId, request)
        );
    }

    @PostMapping("/{reportId}/amend")
    public ApiResponse<ReportAmendmentResponse> amend(
            @PathVariable Long reportId,
            @Valid @RequestBody AmendReportRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic report amended successfully.",
                service.amendReport(reportId, request)
        );
    }

    @GetMapping("/{reportId}/audit")
    public ApiResponse<List<ReportAuditResponse>> audit(@PathVariable Long reportId) {
        return ApiResponse.success(
                "Diagnostic report audit fetched successfully.",
                service.getAudit(reportId)
        );
    }
}