package com.plasmit.radiology.hospital.radiology.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PacsMatchRequest(
        @NotBlank(message = "pacsStudyUid is required.")
        String pacsStudyUid,

        String pacsPatientId,
        String pacsModality,
        Double matchConfidence
) {
}