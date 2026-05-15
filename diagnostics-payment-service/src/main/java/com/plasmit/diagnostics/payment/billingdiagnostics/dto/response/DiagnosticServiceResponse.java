package com.plasmit.diagnostics.payment.billingdiagnostics.dto.response;

public record DiagnosticServiceResponse(
        Long serviceId,
        String serviceCode,
        String serviceName,
        String department,
        String modality,
        String sampleType,
        Integer turnaroundMinutes,
        Boolean requiresApproval,
        Long pricePaise,
        Double taxPercent
) {
}