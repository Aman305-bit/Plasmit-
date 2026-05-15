package com.plasmit.diagnostic.report.alert.controller;

import com.plasmit.diagnostic.report.alert.dto.request.*;
import com.plasmit.diagnostic.report.alert.dto.response.*;
import com.plasmit.diagnostic.report.alert.service.CriticalAlertService;
import com.plasmit.diagnostic.report.common.response.ApiResponse;
import com.plasmit.diagnostic.report.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin("*")
public class CriticalAlertController {

    private final CriticalAlertService service;

    public CriticalAlertController(CriticalAlertService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/alerts")
    public ApiResponse<PageResponse<CriticalAlertResponse>> getAlerts(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "severity", required = false)
            String severity,

            @RequestParam(value = "search", required = false)
            String search,

            @RequestParam(value = "page", defaultValue = "1")
            Integer page,

            @RequestParam(value = "limit", defaultValue = "25")
            Integer limit
    ) {
        return ApiResponse.success(
                "Critical alerts fetched successfully.",
                service.getAlerts(fromDate, toDate, status, severity, search, page, limit)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/alerts/{alertId}")
    public ApiResponse<CriticalAlertResponse> getAlert(@PathVariable Long alertId) {
        return ApiResponse.success(
                "Critical alert fetched successfully.",
                service.getAlert(alertId)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/{reportId}/alerts")
    public ApiResponse<CriticalAlertResponse> createAlert(
            @PathVariable Long reportId,
            @Valid @RequestBody CreateCriticalAlertRequest request
    ) {
        return ApiResponse.success(
                "Critical alert created successfully.",
                service.createAlert(reportId, request)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/alerts/{alertId}/acknowledge")
    public ApiResponse<CriticalAlertAcknowledgementResponse> acknowledge(
            @PathVariable Long alertId,
            @Valid @RequestBody AcknowledgeCriticalAlertRequest request
    ) {
        return ApiResponse.success(
                "Critical alert acknowledged successfully.",
                service.acknowledge(alertId, request)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/alerts/{alertId}/escalate")
    public ApiResponse<CriticalAlertEscalationResponse> escalate(
            @PathVariable Long alertId,
            @Valid @RequestBody EscalateCriticalAlertRequest request
    ) {
        return ApiResponse.success(
                "Critical alert escalated successfully.",
                service.escalate(alertId, request)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/alerts/{alertId}/close")
    public ApiResponse<CriticalAlertResponse> close(
            @PathVariable Long alertId,
            @Valid @RequestBody CloseCriticalAlertRequest request
    ) {
        return ApiResponse.success(
                "Critical alert closed successfully.",
                service.close(alertId, request)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/alerts/{alertId}/timeline")
    public ApiResponse<List<CriticalAlertEventResponse>> timeline(@PathVariable Long alertId) {
        return ApiResponse.success(
                "Critical alert timeline fetched successfully.",
                service.timeline(alertId)
        );
    }
}