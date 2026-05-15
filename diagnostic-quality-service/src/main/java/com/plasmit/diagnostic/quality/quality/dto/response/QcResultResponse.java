package com.plasmit.diagnostic.quality.quality.dto.response;

public record QcResultResponse(
        Long id,
        Long qcEventId,
        String parameterName,
        String expectedValue,
        String observedValue,
        String unit,
        String resultStatus,
        String remarks
) {
}