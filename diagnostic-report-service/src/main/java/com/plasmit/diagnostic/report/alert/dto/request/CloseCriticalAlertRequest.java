package com.plasmit.diagnostic.report.alert.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CloseCriticalAlertRequest(

        @NotBlank(message = "closeNotes is required.")
        String closeNotes
) {
}