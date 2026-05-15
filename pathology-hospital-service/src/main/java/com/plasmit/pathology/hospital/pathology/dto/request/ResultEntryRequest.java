package com.plasmit.pathology.hospital.pathology.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ResultEntryRequest(
        String resultSummary,
        Boolean abnormalFlag,
        Boolean criticalFlag,

        @Valid
        @NotEmpty(message = "parameters are required.")
        List<ResultParameterRequest> parameters
) {
}