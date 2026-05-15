package com.plasmit.diagnostic.report.reports.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReportObservationRequest(
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