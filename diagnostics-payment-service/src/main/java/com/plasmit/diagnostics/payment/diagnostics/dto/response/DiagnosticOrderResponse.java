package com.plasmit.diagnostics.payment.diagnostics.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DiagnosticOrderResponse(
        Long orderId,
        String orderNo,

        Long patientId,
        String patientUhid,
        String patientName,

        Long encounterId,
        String encounterType,
        String source,

        String department,
        String priority,
        String status,

        String clinicalNotes,
        List<String> diagnosisCodes,
        String payerContextId,

        LocalDateTime preferredAt,
        LocalDateTime dueAt,

        List<String> flags,

        String accessionNo,
        String barcodeNo,

        String billingAuthorizationStatus,
        Boolean billingReleaseBlocked,
        Long invoiceId,

        List<DiagnosticOrderLineResponse> lines,

        Long createdBy,
        LocalDateTime createdAt
) {
}