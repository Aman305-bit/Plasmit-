package com.plasmit.pathology.hospital.pathology.dto.response;

public record ResultParameterResponse(
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