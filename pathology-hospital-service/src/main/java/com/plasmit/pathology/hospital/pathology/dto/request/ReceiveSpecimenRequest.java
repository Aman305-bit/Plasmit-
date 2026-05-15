package com.plasmit.pathology.hospital.pathology.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReceiveSpecimenRequest(
        @NotBlank(message = "receivedByName is required.")
        String receivedByName,

        String notes
) {
}