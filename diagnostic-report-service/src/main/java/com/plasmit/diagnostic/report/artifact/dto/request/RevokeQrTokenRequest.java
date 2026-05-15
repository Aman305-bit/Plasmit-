package com.plasmit.diagnostic.report.artifact.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RevokeQrTokenRequest(
        @NotBlank(message = "reason is required.")
        String reason
) {
}