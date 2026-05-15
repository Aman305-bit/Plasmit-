package com.plasmit.diagnostic.report.alert.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EscalateCriticalAlertRequest(

        @NotNull(message = "escalatedToUserId is required.")
        Long escalatedToUserId,

        @NotBlank(message = "escalatedToName is required.")
        String escalatedToName,

        String escalatedToRole,
        String escalatedToMobile,
        String escalatedToEmail,

        @NotBlank(message = "escalationReason is required.")
        String escalationReason
) {
}