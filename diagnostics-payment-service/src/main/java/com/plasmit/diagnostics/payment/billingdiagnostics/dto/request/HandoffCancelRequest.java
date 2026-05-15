package com.plasmit.diagnostics.payment.billingdiagnostics.dto.request;

import jakarta.validation.constraints.NotBlank;

public record HandoffCancelRequest(
        @NotBlank(message = "reason is required.")
        String reason
) {
}