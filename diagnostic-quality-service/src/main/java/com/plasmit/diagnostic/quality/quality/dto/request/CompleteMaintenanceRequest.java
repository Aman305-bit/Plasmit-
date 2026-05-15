package com.plasmit.diagnostic.quality.quality.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompleteMaintenanceRequest(
        @NotBlank(message = "performedBy is required.")
        String performedBy,

        @NotBlank(message = "resolutionNotes is required.")
        String resolutionNotes
) {
}