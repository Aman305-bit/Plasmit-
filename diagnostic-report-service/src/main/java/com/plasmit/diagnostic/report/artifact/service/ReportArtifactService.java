package com.plasmit.diagnostic.report.artifact.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasmit.diagnostic.report.artifact.dto.request.*;
import com.plasmit.diagnostic.report.artifact.dto.response.*;
import com.plasmit.diagnostic.report.artifact.repository.ReportArtifactRepository;
import com.plasmit.diagnostic.report.artifact.validator.ReportArtifactValidator;
import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.context.TenantContext;
import com.plasmit.diagnostic.report.reports.dto.response.DiagnosticReportResponse;
import com.plasmit.diagnostic.report.validator.CommonRequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class ReportArtifactService {

    private final ReportArtifactRepository repository;
    private final ReportArtifactValidator validator;
    private final CommonRequestValidator commonRequestValidator;
    private final ObjectMapper objectMapper;

    public ReportArtifactService(ReportArtifactRepository repository,
                                 ReportArtifactValidator validator,
                                 CommonRequestValidator commonRequestValidator,
                                 ObjectMapper objectMapper) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReportTemplateResponse createTemplate(CreateReportTemplateRequest request) {
        validateContext();
        validator.validateTemplate(request);

        return repository.createTemplate(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                request
        );
    }

    public List<ReportTemplateResponse> getTemplates(String department,
                                                     String modality,
                                                     String status) {
        validateContext();
        validator.validateTemplateStatus(status);

        return repository.findTemplates(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                department,
                modality,
                status
        );
    }

    public ReportTemplateResponse getTemplate(Long templateId) {
        validateContext();
        validator.validateTemplateId(templateId);

        return repository.findTemplateById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                templateId
        );
    }

    @Transactional
    public ReportVersionResponse createVersion(Long reportId,
                                               CreateReportVersionRequest request) {
        validateContext();
        validator.validateReportId(reportId);

        DiagnosticReportResponse report = repository.getReport(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(report);
        } catch (Exception ex) {
            throw ApiException.business("Unable to create report snapshot.");
        }

        return repository.createVersion(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                reportId,
                snapshotJson,
                request
        );
    }

    public List<ReportVersionResponse> getVersions(Long reportId) {
        validateContext();
        validator.validateReportId(reportId);

        repository.getReport(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );

        return repository.findVersions(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );
    }

    @Transactional
    public PdfArtifactResponse generatePdfArtifact(Long reportId,
                                                   GeneratePdfArtifactRequest request) {
        validateContext();
        validator.validateReportId(reportId);

        DiagnosticReportResponse report = repository.getReport(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );

        if (!"Released".equals(report.reportStatus())
                && !"Signed".equals(report.reportStatus())
                && !"Amended".equals(report.reportStatus())) {
            throw ApiException.workflow("PDF artifact can be generated only for Signed, Released or Amended report.");
        }

        if (request != null && request.versionId() != null) {
            repository.findVersionById(
                    TenantContext.getTenantId(),
                    TenantContext.getHospitalId(),
                    TenantContext.getBranchId(),
                    request.versionId()
            );
        }

        String artifactNo = "PDF-" + System.currentTimeMillis();
        String fileName = report.reportNo() + ".pdf";

        String contentHash = repository.hash(report.reportNo() + "|" + report.patientName() + "|" + report.reportStatus());

        return repository.createPdfArtifact(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                reportId,
                request,
                artifactNo,
                fileName,
                contentHash
        );
    }

    public List<PdfArtifactResponse> getArtifacts(Long reportId) {
        validateContext();
        validator.validateReportId(reportId);

        repository.getReport(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );

        return repository.findArtifacts(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );
    }

    @Transactional
    public QrTokenResponse createQrToken(Long reportId,
                                         CreateQrTokenRequest request) {
        validateContext();
        validator.validateReportId(reportId);
        validator.validateQrToken(request);

        DiagnosticReportResponse report = repository.getReport(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );

        if (!"Released".equals(report.reportStatus())) {
            throw ApiException.workflow("QR verification token can be generated only for Released report.");
        }

        if (request != null && request.artifactId() != null) {
            repository.findArtifactById(
                    TenantContext.getTenantId(),
                    TenantContext.getHospitalId(),
                    TenantContext.getBranchId(),
                    request.artifactId()
            );
        }

        String token = generateSecureToken();
        String verificationUrl = "http://localhost:8098/api/v1/hospital/diagnostic-reports/verify/" + token;

        return repository.createQrToken(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                reportId,
                request,
                token,
                verificationUrl
        );
    }

    @Transactional
    public QrTokenResponse revokeQrToken(Long tokenId,
                                         RevokeQrTokenRequest request) {
        validateContext();
        validator.validateTokenId(tokenId);
        validator.validateRevoke(request);

        return repository.revokeQrToken(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                tokenId,
                request.reason()
        );
    }

    @Transactional
    public ReportVerificationResponse verifyReport(String qrToken,
                                                   HttpServletRequest request) {
        validator.validateQrTokenValue(qrToken);

        ReportArtifactRepository.QrTokenContext tokenContext = repository.findQrTokenContext(qrToken);

        String status = "Verified";

        if (!"Active".equals(tokenContext.status())) {
            status = "Invalid";
            repository.insertVerificationLog(
                    tokenContext.tenantId(),
                    tokenContext.hospitalId(),
                    tokenContext.branchId(),
                    tokenContext.reportId(),
                    tokenContext.tokenId(),
                    status,
                    "PublicVerifier",
                    clientIp(request),
                    request.getHeader("User-Agent"),
                    request.getHeader("X-Request-Id")
            );
            throw ApiException.workflow("QR token is not active.");
        }

        if (tokenContext.expiresAt() != null && tokenContext.expiresAt().isBefore(LocalDateTime.now())) {
            status = "Expired";
            repository.insertVerificationLog(
                    tokenContext.tenantId(),
                    tokenContext.hospitalId(),
                    tokenContext.branchId(),
                    tokenContext.reportId(),
                    tokenContext.tokenId(),
                    status,
                    "PublicVerifier",
                    clientIp(request),
                    request.getHeader("User-Agent"),
                    request.getHeader("X-Request-Id")
            );
            throw ApiException.workflow("QR token is expired.");
        }

        DiagnosticReportResponse report = repository.getReport(
                tokenContext.tenantId(),
                tokenContext.hospitalId(),
                tokenContext.branchId(),
                tokenContext.reportId()
        );

        PdfArtifactResponse artifact = null;

        if (tokenContext.artifactId() != null) {
            artifact = repository.findArtifactById(
                    tokenContext.tenantId(),
                    tokenContext.hospitalId(),
                    tokenContext.branchId(),
                    tokenContext.artifactId()
            );
        } else {
            artifact = repository.findLatestArtifactNullable(
                    tokenContext.tenantId(),
                    tokenContext.hospitalId(),
                    tokenContext.branchId(),
                    tokenContext.reportId()
            );
        }

        repository.insertVerificationLog(
                tokenContext.tenantId(),
                tokenContext.hospitalId(),
                tokenContext.branchId(),
                tokenContext.reportId(),
                tokenContext.tokenId(),
                status,
                "PublicVerifier",
                clientIp(request),
                request.getHeader("User-Agent"),
                request.getHeader("X-Request-Id")
        );

        return new ReportVerificationResponse(
                status,
                tokenContext.tokenId(),
                report.reportId(),
                report.reportNo(),
                report.patientName(),
                report.reportStatus(),
                artifact == null ? null : artifact.fileUrl(),
                report
        );
    }

    public List<VerificationLogResponse> getVerificationLogs(Long reportId) {
        validateContext();
        validator.validateReportId(reportId);

        repository.getReport(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );

        return repository.findVerificationLogs(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}