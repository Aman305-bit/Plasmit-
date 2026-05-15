package com.plasmit.pathology.hospital.pathology.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CollectSpecimenRequest(
        @NotBlank(message = "collectedByName is required.")
        String collectedByName,

        String notes
) {
}