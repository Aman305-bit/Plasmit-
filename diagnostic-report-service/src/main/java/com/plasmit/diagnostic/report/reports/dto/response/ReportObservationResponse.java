package com.plasmit.diagnostic.report.reports.dto.response;

public record ReportObservationResponse(
        Long id,
        String parameterCode,
        String parameterName,
        String resultValue,
        String unit,
        String referenceRange,
        Boolean abnormalFlag,
        Boolean criticalFlag,
        Integer displayOrder
) {
}