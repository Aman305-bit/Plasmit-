package com.plasmit.pathology.hospital.pathology.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerificationRequest(
        @NotBlank(message = "verifierName is required.")
        String verifierName,

        String remarks
) {
}