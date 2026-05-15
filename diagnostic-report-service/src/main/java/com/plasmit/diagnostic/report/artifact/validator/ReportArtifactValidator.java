package com.plasmit.diagnostic.report.artifact.validator;

import com.plasmit.diagnostic.report.artifact.dto.request.*;
import com.plasmit.diagnostic.report.common.exception.ApiException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class ReportArtifactValidator {

    private static final Set<String> TEMPLATE_STATUSES = Set.of("Active", "Inactive");

    public void validateReportId(Long reportId) {
        if (reportId == null || reportId <= 0) {
            throw ApiException.validation("Valid reportId is required.");
        }
    }

    public void validateTemplateId(Long templateId) {
        if (templateId == null || templateId <= 0) {
            throw ApiException.validation("Valid templateId is required.");
        }
    }

    public void validateArtifactId(Long artifactId) {
        if (artifactId == null || artifactId <= 0) {
            throw ApiException.validation("Valid artifactId is required.");
        }
    }

    public void validateTokenId(Long tokenId) {
        if (tokenId == null || tokenId <= 0) {
            throw ApiException.validation("Valid tokenId is required.");
        }
    }

    public void validateTemplate(CreateReportTemplateRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.templateCode() == null || request.templateCode().isBlank()) {
            throw ApiException.validation("templateCode is required.");
        }

        if (request.templateName() == null || request.templateName().isBlank()) {
            throw ApiException.validation("templateName is required.");
        }

        if (request.department() == null || request.department().isBlank()) {
            throw ApiException.validation("department is required.");
        }

        if (request.templateBody() == null || request.templateBody().isBlank()) {
            throw ApiException.validation("templateBody is required.");
        }
    }

    public void validateTemplateStatus(String status) {
        if (status != null && !status.isBlank() && !TEMPLATE_STATUSES.contains(status)) {
            throw ApiException.validation("Invalid template status.");
        }
    }

    public void validateQrToken(CreateQrTokenRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.expiresAt() != null && !request.expiresAt().isAfter(LocalDateTime.now())) {
            throw ApiException.validation("expiresAt must be in future.");
        }
    }

    public void validateQrTokenValue(String qrToken) {
        if (qrToken == null || qrToken.isBlank()) {
            throw ApiException.validation("qrToken is required.");
        }
    }

    public void validateRevoke(RevokeQrTokenRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw ApiException.validation("reason is required.");
        }
    }
}