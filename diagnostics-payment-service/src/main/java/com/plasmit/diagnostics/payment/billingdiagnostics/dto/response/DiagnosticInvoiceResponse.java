package com.plasmit.diagnostics.payment.billingdiagnostics.dto.response;

import java.util.List;

public record DiagnosticInvoiceResponse(
        Long invoiceId,
        String invoiceNo,
        String status,
        Long grossPaise,
        Long discountPaise,
        Long taxPaise,
        Long netPaise,
        Long paidPaise,
        Long duePaise,
        List<Long> handoffIds
) {
}