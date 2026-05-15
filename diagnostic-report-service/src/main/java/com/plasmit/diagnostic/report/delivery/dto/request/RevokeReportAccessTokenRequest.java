package com.plasmit.diagnostic.report.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RevokeReportAccessTokenRequest(
        @NotBlank(message = "reason is required.")
        String reason
) {
}