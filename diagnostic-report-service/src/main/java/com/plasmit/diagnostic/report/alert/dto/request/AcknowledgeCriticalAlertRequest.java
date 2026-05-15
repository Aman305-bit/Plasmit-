package com.plasmit.diagnostic.report.alert.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AcknowledgeCriticalAlertRequest(

        @NotBlank(message = "acknowledgedByName is required.")
        String acknowledgedByName,

        String acknowledgementNotes
) {
}