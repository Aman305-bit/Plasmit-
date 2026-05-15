package com.plasmit.diagnostic.quality.quality.dto.request;

import jakarta.validation.constraints.NotBlank;

public record QcResultRequest(
        @NotBlank(message = "parameterName is required.")
        String parameterName,

        String expectedValue,
        String observedValue,
        String unit,

        @NotBlank(message = "resultStatus is required.")
        String resultStatus,

        String remarks
) {
}