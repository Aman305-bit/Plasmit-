package com.plasmit.diagnostic.report.delivery.service;

import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.context.TenantContext;
import com.plasmit.diagnostic.report.delivery.dto.request.CreateReportAccessTokenRequest;
import com.plasmit.diagnostic.report.delivery.dto.request.RevokeReportAccessTokenRequest;
import com.plasmit.diagnostic.report.delivery.dto.request.SendReportDeliveryRequest;
import com.plasmit.diagnostic.report.delivery.dto.response.*;
import com.plasmit.diagnostic.report.delivery.repository.ReportDeliveryRepository;
import com.plasmit.diagnostic.report.delivery.validator.ReportDeliveryValidator;
import com.plasmit.diagnostic.report.reports.dto.response.DiagnosticReportResponse;
import com.plasmit.diagnostic.report.validator.CommonRequestValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class ReportDeliveryService {

    private final ReportDeliveryRepository repository;
    private final ReportDeliveryValidator validator;
    private final CommonRequestValidator commonRequestValidator;

    public ReportDeliveryService(ReportDeliveryRepository repository,
                                 ReportDeliveryValidator validator,
                                 CommonRequestValidator commonRequestValidator) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
    }

    @Transactional
    public ReportDeliveryResponse sendReport(Long reportId,
                                             SendReportDeliveryRequest request) {
        validateContext();
        validator.validateReportId(reportId);
        validator.validateSend(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        DiagnosticReportResponse report = repository.getReport(tenantId, hospitalId, branchId, reportId);

        if (!"Released".equals(report.reportStatus())) {
            throw ApiException.workflow("Only Released report can be delivered.");
        }

        String deliveryReference = "DLV-" + UUID.randomUUID();

        ReportDeliveryResponse response = repository.createDelivery(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                TenantContext.getUserId(),
                request,
                "Sent",
                deliveryReference,
                null
        );

        repository.insertAccessLog(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                null,
                "DeliverySent",
                String.valueOf(TenantContext.getUserId()),
                null,
                null,
                TenantContext.getRequestId()
        );

        return response;
    }

    public ReportDeliveryStatusResponse getDeliveryStatus(Long reportId) {
        validateContext();
        validator.validateReportId(reportId);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        DiagnosticReportResponse report = repository.getReport(tenantId, hospitalId, branchId, reportId);

        repository.expireOldTokens();

        List<ReportDeliveryResponse> deliveries = repository.findDeliveriesByReport(
                tenantId,
                hospitalId,
                branchId,
                reportId
        );

        List<ReportAccessTokenResponse> tokens = repository.findAccessTokensByReport(
                tenantId,
                hospitalId,
                branchId,
                reportId
        );

        return new ReportDeliveryStatusResponse(
                reportId,
                report.reportStatus(),
                deliveries,
                tokens
        );
    }

    @Transactional
    public ReportAccessTokenResponse createAccessToken(Long reportId,
                                                       CreateReportAccessTokenRequest request) {
        validateContext();
        validator.validateReportId(reportId);
        validator.validateCreateToken(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        DiagnosticReportResponse report = repository.getReport(tenantId, hospitalId, branchId, reportId);

        if (!"Released".equals(report.reportStatus())) {
            throw ApiException.workflow("Only Released report can generate access token.");
        }

        String token = generateSecureToken();
        String accessUrl = "https://reports.plasmit.local/diagnostic-reports/access/" + token;

        ReportAccessTokenResponse response = repository.createAccessToken(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                TenantContext.getUserId(),
                request,
                token,
                accessUrl
        );

        repository.insertAccessLog(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                response.tokenId(),
                "TokenCreated",
                String.valueOf(TenantContext.getUserId()),
                null,
                null,
                TenantContext.getRequestId()
        );

        return response;
    }

    @Transactional
    public ReportAccessTokenResponse revokeAccessToken(Long reportId,
                                                       Long tokenId,
                                                       RevokeReportAccessTokenRequest request) {
        validateContext();
        validator.validateReportId(reportId);
        validator.validateTokenId(tokenId);
        validator.validateRevoke(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        repository.getReport(tenantId, hospitalId, branchId, reportId);

        ReportAccessTokenResponse response = repository.revokeToken(
                tenantId,
                hospitalId,
                branchId,
                tokenId,
                TenantContext.getUserId(),
                request.reason()
        );

        repository.insertAccessLog(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                tokenId,
                "TokenRevoked",
                String.valueOf(TenantContext.getUserId()),
                null,
                null,
                TenantContext.getRequestId()
        );

        return response;
    }

    @Transactional
    public PatientReportAccessResponse accessReport(String accessToken,
                                                    HttpServletRequest servletRequest) {
        validator.validateToken(accessToken);

        repository.expireOldTokens();

        ReportDeliveryRepository.TokenContext tokenContext = repository.findTokenContext(accessToken);

        if (!"Active".equals(tokenContext.status())) {
            throw ApiException.workflow("Report access token is not active.");
        }

        if (tokenContext.expiresAt() != null && tokenContext.expiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.workflow("Report access token is expired.");
        }

        DiagnosticReportResponse report = repository.getReport(
                tokenContext.tenantId(),
                tokenContext.hospitalId(),
                tokenContext.branchId(),
                tokenContext.reportId()
        );

        repository.insertAccessLog(
                tokenContext.tenantId(),
                tokenContext.hospitalId(),
                tokenContext.branchId(),
                tokenContext.reportId(),
                tokenContext.tokenId(),
                "ReportViewed",
                "PublicAccess",
                clientIp(servletRequest),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getHeader("X-Request-Id")
        );

        return new PatientReportAccessResponse(
                tokenContext.tokenId(),
                "Allowed",
                report
        );
    }

    public List<ReportAccessLogResponse> getLogs(Long reportId) {
        validateContext();
        validator.validateReportId(reportId);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        repository.getReport(tenantId, hospitalId, branchId, reportId);

        return repository.findAccessLogs(tenantId, hospitalId, branchId, reportId);
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