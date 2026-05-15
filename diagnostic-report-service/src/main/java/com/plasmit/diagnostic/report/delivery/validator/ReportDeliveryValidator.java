package com.plasmit.diagnostic.report.delivery.validator;

import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.delivery.dto.request.CreateReportAccessTokenRequest;
import com.plasmit.diagnostic.report.delivery.dto.request.RevokeReportAccessTokenRequest;
import com.plasmit.diagnostic.report.delivery.dto.request.SendReportDeliveryRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class ReportDeliveryValidator {

    private static final Set<String> DELIVERY_CHANNELS = Set.of(
            "Portal",
            "Email",
            "SMS",
            "WhatsApp",
            "Print"
    );

    private static final Set<String> RECIPIENT_TYPES = Set.of(
            "Patient",
            "Doctor",
            "HospitalStaff",
            "ExternalConsultant"
    );

    public void validateReportId(Long reportId) {
        if (reportId == null || reportId <= 0) {
            throw ApiException.validation("Valid reportId is required.");
        }
    }

    public void validateSend(SendReportDeliveryRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.deliveryChannel() == null || request.deliveryChannel().isBlank()) {
            throw ApiException.validation("deliveryChannel is required.");
        }

        if (!DELIVERY_CHANNELS.contains(request.deliveryChannel())) {
            throw ApiException.validation("Invalid deliveryChannel.");
        }

        if (request.recipientType() == null || request.recipientType().isBlank()) {
            throw ApiException.validation("recipientType is required.");
        }

        if (!RECIPIENT_TYPES.contains(request.recipientType())) {
            throw ApiException.validation("Invalid recipientType.");
        }

        if (("Email".equals(request.deliveryChannel()) && isBlank(request.recipientEmail()))
                || ("SMS".equals(request.deliveryChannel()) && isBlank(request.recipientMobile()))
                || ("WhatsApp".equals(request.deliveryChannel()) && isBlank(request.recipientMobile()))) {
            throw ApiException.validation("Recipient contact is required for selected delivery channel.");
        }
    }

    public void validateCreateToken(CreateReportAccessTokenRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.expiresAt() == null) {
            throw ApiException.validation("expiresAt is required.");
        }

        if (!request.expiresAt().isAfter(LocalDateTime.now())) {
            throw ApiException.validation("expiresAt must be in future.");
        }

        if (!isBlank(request.recipientType()) && !RECIPIENT_TYPES.contains(request.recipientType())) {
            throw ApiException.validation("Invalid recipientType.");
        }
    }

    public void validateToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw ApiException.validation("accessToken is required.");
        }
    }

    public void validateTokenId(Long tokenId) {
        if (tokenId == null || tokenId <= 0) {
            throw ApiException.validation("Valid tokenId is required.");
        }
    }

    public void validateRevoke(RevokeReportAccessTokenRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw ApiException.validation("reason is required.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}