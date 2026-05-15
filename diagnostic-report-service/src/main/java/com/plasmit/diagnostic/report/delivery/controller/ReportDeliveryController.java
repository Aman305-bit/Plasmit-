package com.plasmit.diagnostic.report.delivery.controller;

import com.plasmit.diagnostic.report.common.response.ApiResponse;
import com.plasmit.diagnostic.report.delivery.dto.request.CreateReportAccessTokenRequest;
import com.plasmit.diagnostic.report.delivery.dto.request.RevokeReportAccessTokenRequest;
import com.plasmit.diagnostic.report.delivery.dto.request.SendReportDeliveryRequest;
import com.plasmit.diagnostic.report.delivery.dto.response.*;
import com.plasmit.diagnostic.report.delivery.service.ReportDeliveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
public class ReportDeliveryController {

    private final ReportDeliveryService service;

    public ReportDeliveryController(ReportDeliveryService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/{reportId}/delivery/send")
    public ApiResponse<ReportDeliveryResponse> sendReport(
            @PathVariable Long reportId,
            @Valid @RequestBody SendReportDeliveryRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic report delivery sent successfully.",
                service.sendReport(reportId, request)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/{reportId}/delivery/status")
    public ApiResponse<ReportDeliveryStatusResponse> deliveryStatus(
            @PathVariable Long reportId
    ) {
        return ApiResponse.success(
                "Diagnostic report delivery status fetched successfully.",
                service.getDeliveryStatus(reportId)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/{reportId}/delivery/access-token")
    public ApiResponse<ReportAccessTokenResponse> createAccessToken(
            @PathVariable Long reportId,
            @Valid @RequestBody CreateReportAccessTokenRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic report access token created successfully.",
                service.createAccessToken(reportId, request)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/{reportId}/delivery/access-token/{tokenId}/revoke")
    public ApiResponse<ReportAccessTokenResponse> revokeAccessToken(
            @PathVariable Long reportId,
            @PathVariable Long tokenId,
            @Valid @RequestBody RevokeReportAccessTokenRequest request
    ) {
        return ApiResponse.success(
                "Diagnostic report access token revoked successfully.",
                service.revokeAccessToken(reportId, tokenId, request)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/delivery/access/{accessToken}")
    public ApiResponse<PatientReportAccessResponse> accessReport(
            @PathVariable String accessToken,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                "Diagnostic report accessed successfully.",
                service.accessReport(accessToken, servletRequest)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/{reportId}/delivery/logs")
    public ApiResponse<List<ReportAccessLogResponse>> logs(
            @PathVariable Long reportId
    ) {
        return ApiResponse.success(
                "Diagnostic report delivery/access logs fetched successfully.",
                service.getLogs(reportId)
        );
    }
}