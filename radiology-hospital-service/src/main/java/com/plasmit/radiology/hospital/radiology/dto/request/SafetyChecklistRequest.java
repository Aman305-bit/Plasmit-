package com.plasmit.radiology.hospital.radiology.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SafetyChecklistRequest(
        String pregnancyCheck,
        String contrastAllergyCheck,
        String renalFunctionCheck,
        String metalImplantCheck,
        Boolean consentTaken,

        @NotBlank(message = "remarks is required.")
        String remarks
) {
}