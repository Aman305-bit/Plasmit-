package com.plasmit.diagnostics.payment.billingdiagnostics.dto.response;

import java.util.List;

public record DiagnosticCartValidateResponse(
        List<DiagnosticCartLineResponse> lines,
        Long grossPaise,
        Long discountPaise,
        Long taxPaise,
        Long netPaise
) {
}