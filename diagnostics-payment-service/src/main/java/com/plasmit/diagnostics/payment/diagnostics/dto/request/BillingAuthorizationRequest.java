package com.plasmit.diagnostics.payment.diagnostics.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BillingAuthorizationRequest(
        @NotBlank(message = "action is required.")
        String action,

        String reason
) {
}