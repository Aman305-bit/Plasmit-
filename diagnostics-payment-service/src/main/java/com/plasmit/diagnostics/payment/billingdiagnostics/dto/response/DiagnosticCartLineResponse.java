package com.plasmit.diagnostics.payment.billingdiagnostics.dto.response;

public record DiagnosticCartLineResponse(
        Long serviceId,
        String serviceCode,
        String serviceName,
        String department,
        String modality,
        Integer quantity,
        Long unitPricePaise,
        Long grossPaise,
        Long discountPaise,
        Long taxPaise,
        Long netPaise
) {
}