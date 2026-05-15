package com.plasmit.diagnostics.payment.billingdiagnostics.dto.response;

import java.util.List;

public record BillingDiagnosticBootstrapResponse(
        List<String> departments,
        List<String> modalities,
        List<String> payerTypes
) {
}