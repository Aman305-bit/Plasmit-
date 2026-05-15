package com.plasmit.pathology.hospital.pathology.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectSpecimenRequest(
        @NotBlank(message = "reason is required.")
        String reason,

        String notes
) {
}