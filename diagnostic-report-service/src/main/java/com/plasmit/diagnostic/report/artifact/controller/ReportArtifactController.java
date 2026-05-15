package com.plasmit.diagnostic.report.artifact.controller;

import com.plasmit.diagnostic.report.artifact.dto.request.*;
import com.plasmit.diagnostic.report.artifact.dto.response.*;
import com.plasmit.diagnostic.report.artifact.service.ReportArtifactService;
import com.plasmit.diagnostic.report.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
public class ReportArtifactController {

    private final ReportArtifactService service;

    public ReportArtifactController(ReportArtifactService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/templates")
    public ApiResponse<ReportTemplateResponse> createTemplate(
            @Valid @RequestBody CreateReportTemplateRequest request
    ) {
        return ApiResponse.success(
                "Report template created successfully.",
                service.createTemplate(request)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/templates")
    public ApiResponse<List<ReportTemplateResponse>> getTemplates(
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "modality", required = false) String modality,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(
                "Report templates fetched successfully.",
                service.getTemplates(department, modality, status)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/templates/{templateId}")
    public ApiResponse<ReportTemplateResponse> getTemplate(@PathVariable Long templateId) {
        return ApiResponse.success(
                "Report template fetched successfully.",
                service.getTemplate(templateId)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/{reportId}/versions")
    public ApiResponse<ReportVersionResponse> createVersion(
            @PathVariable Long reportId,
            @RequestBody(required = false) CreateReportVersionRequest request
    ) {
        return ApiResponse.success(
                "Report version created successfully.",
                service.createVersion(reportId, request)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/{reportId}/versions")
    public ApiResponse<List<ReportVersionResponse>> getVersions(@PathVariable Long reportId) {
        return ApiResponse.success(
                "Report versions fetched successfully.",
                service.getVersions(reportId)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/{reportId}/artifacts/pdf")
    public ApiResponse<PdfArtifactResponse> generatePdfArtifact(
            @PathVariable Long reportId,
            @RequestBody(required = false) GeneratePdfArtifactRequest request
    ) {
        return ApiResponse.success(
                "PDF artifact generated successfully.",
                service.generatePdfArtifact(reportId, request)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/{reportId}/artifacts")
    public ApiResponse<List<PdfArtifactResponse>> getArtifacts(@PathVariable Long reportId) {
        return ApiResponse.success(
                "Report PDF artifacts fetched successfully.",
                service.getArtifacts(reportId)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/{reportId}/qr-token")
    public ApiResponse<QrTokenResponse> createQrToken(
            @PathVariable Long reportId,
            @RequestBody(required = false) CreateQrTokenRequest request
    ) {
        return ApiResponse.success(
                "Report QR verification token created successfully.",
                service.createQrToken(reportId, request)
        );
    }

    @PostMapping("/api/v1/hospital/diagnostic-reports/qr-token/{tokenId}/revoke")
    public ApiResponse<QrTokenResponse> revokeQrToken(
            @PathVariable Long tokenId,
            @Valid @RequestBody RevokeQrTokenRequest request
    ) {
        return ApiResponse.success(
                "Report QR token revoked successfully.",
                service.revokeQrToken(tokenId, request)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/verify/{qrToken}")
    public ApiResponse<ReportVerificationResponse> verifyReport(
            @PathVariable String qrToken,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "Report verified successfully.",
                service.verifyReport(qrToken, request)
        );
    }

    @GetMapping("/api/v1/hospital/diagnostic-reports/{reportId}/verification-logs")
    public ApiResponse<List<VerificationLogResponse>> getVerificationLogs(@PathVariable Long reportId) {
        return ApiResponse.success(
                "Report verification logs fetched successfully.",
                service.getVerificationLogs(reportId)
        );
    }
}