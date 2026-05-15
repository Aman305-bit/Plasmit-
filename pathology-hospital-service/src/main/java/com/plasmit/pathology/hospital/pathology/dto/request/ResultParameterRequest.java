package com.plasmit.pathology.hospital.pathology.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResultParameterRequest(
        String parameterCode,

        @NotBlank(message = "parameterName is required.")
        String parameterName,

        String resultValue,
        String unit,
        String referenceRange,
        Boolean abnormalFlag,
        Boolean criticalFlag,
        Integer displayOrder
) {
}