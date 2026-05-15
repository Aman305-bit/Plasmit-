package com.plasmit.diagnostic.report.delivery.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateReportAccessTokenRequest(
        String recipientType,
        String recipientName,
        String recipientMobile,
        String recipientEmail,

        @NotNull(message = "expiresAt is required.")
        @Future(message = "expiresAt must be in future.")
        LocalDateTime expiresAt
) {
}