package com.plasmit.diagnostic.integration.integration.controller;

import com.plasmit.diagnostic.integration.common.response.ApiResponse;
import com.plasmit.diagnostic.integration.common.response.PageResponse;
import com.plasmit.diagnostic.integration.integration.dto.request.*;
import com.plasmit.diagnostic.integration.integration.dto.response.*;
import com.plasmit.diagnostic.integration.integration.service.DiagnosticIntegrationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital/diagnostic-integrations")
@CrossOrigin("*")
public class DiagnosticIntegrationController {

    private final DiagnosticIntegrationService service;

    public DiagnosticIntegrationController(DiagnosticIntegrationService service) {
        this.service = service;
    }

    @GetMapping("/nodes")
    public ApiResponse<List<IntegrationNodeResponse>> getNodes(
            @RequestParam(value = "integrationType", required = false) String integrationType,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(
                "Integration nodes fetched successfully.",
                service.getNodes(integrationType, status)
        );
    }

    @GetMapping("/nodes/{nodeId}")
    public ApiResponse<IntegrationNodeResponse> getNode(@PathVariable Long nodeId) {
        return ApiResponse.success(
                "Integration node fetched successfully.",
                service.getNode(nodeId)
        );
    }

    @PostMapping("/nodes/{nodeId}/heartbeat")
    public ApiResponse<IntegrationNodeResponse> heartbeat(
            @PathVariable Long nodeId,
            @RequestBody(required = false) HeartbeatRequest request
    ) {
        return ApiResponse.success(
                "Integration node heartbeat updated successfully.",
                service.heartbeat(nodeId, request)
        );
    }

    @PostMapping("/events")
    public ApiResponse<ExchangeEventResponse> createEvent(
            @Valid @RequestBody CreateExchangeEventRequest request
    ) {
        return ApiResponse.success(
                "Integration exchange event created successfully.",
                service.createEvent(request)
        );
    }

    @GetMapping("/events")
    public ApiResponse<PageResponse<ExchangeEventResponse>> getEvents(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "nodeId", required = false) Long nodeId,
            @RequestParam(value = "search", required = false) String search,

            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "limit", defaultValue = "25") Integer limit
    ) {
        return ApiResponse.success(
                "Integration exchange events fetched successfully.",
                service.getEvents(fromDate, toDate, status, eventType, nodeId, search, page, limit)
        );
    }

    @GetMapping("/events/{eventId}")
    public ApiResponse<ExchangeEventResponse> getEvent(@PathVariable Long eventId) {
        return ApiResponse.success(
                "Integration exchange event fetched successfully.",
                service.getEvent(eventId)
        );
    }

    @PostMapping("/events/{eventId}/process")
    public ApiResponse<ExchangeEventResponse> processEvent(
            @PathVariable Long eventId,
            @RequestBody(required = false) ProcessExchangeEventRequest request
    ) {
        return ApiResponse.success(
                "Integration exchange event processed successfully.",
                service.processEvent(eventId, request)
        );
    }

    @PostMapping("/events/{eventId}/fail")
    public ApiResponse<ExchangeEventResponse> failEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody FailExchangeEventRequest request
    ) {
        return ApiResponse.success(
                "Integration exchange event marked as failed.",
                service.failEvent(eventId, request)
        );
    }

    @PostMapping("/events/{eventId}/retry")
    public ApiResponse<RetryLogResponse> retryEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody RetryExchangeEventRequest request
    ) {
        return ApiResponse.success(
                "Integration exchange event retry scheduled successfully.",
                service.retryEvent(eventId, request)
        );
    }

    @GetMapping("/events/{eventId}/retries")
    public ApiResponse<List<RetryLogResponse>> retryLogs(@PathVariable Long eventId) {
        return ApiResponse.success(
                "Integration exchange event retry logs fetched successfully.",
                service.getRetryLogs(eventId)
        );
    }

    @GetMapping("/events/{eventId}/audit")
    public ApiResponse<List<IntegrationAuditResponse>> audit(@PathVariable Long eventId) {
        return ApiResponse.success(
                "Integration exchange event audit fetched successfully.",
                service.getEventAudit(eventId)
        );
    }
}