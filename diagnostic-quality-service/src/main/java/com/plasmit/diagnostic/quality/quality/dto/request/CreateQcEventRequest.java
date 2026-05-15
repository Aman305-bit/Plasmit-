package com.plasmit.diagnostic.quality.quality.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateQcEventRequest(
        Long equipmentId,

        @NotBlank(message = "qcType is required.")
        String qcType,

        @NotBlank(message = "department is required.")
        String department,

        String modality,
        String performedBy,
        String remarks,

        @Valid
        @NotEmpty(message = "results are required.")
        List<QcResultRequest> results
) {
}