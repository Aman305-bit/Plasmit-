package com.plasmit.diagnostics.payment.diagnostics.dto.response;

public record DiagnosticOrderLineResponse(
        Long lineId,
        Long serviceId,
        String serviceCode,
        String serviceName,
        String department,
        String modality,
        String status
) {
}