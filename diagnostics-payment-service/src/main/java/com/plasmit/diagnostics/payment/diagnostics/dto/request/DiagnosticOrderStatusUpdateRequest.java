package com.plasmit.diagnostics.payment.diagnostics.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DiagnosticOrderStatusUpdateRequest(
        @NotBlank(message = "status is required.")
        String status,

        String reason,
        String notes
) {
}