package com.plasmit.diagnostics.payment.diagnostics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record DiagnosticOrderCreateRequest(
        @NotNull(message = "patientId is required.")
        Long patientId,

        String patientUhid,

        @NotBlank(message = "patientName is required.")
        String patientName,

        Long encounterId,
        String encounterType,

        @NotBlank(message = "source is required.")
        String source,

        @NotBlank(message = "department is required.")
        String department,

        @NotEmpty(message = "serviceIds are required.")
        List<Long> serviceIds,

        @NotBlank(message = "priority is required.")
        String priority,

        String clinicalNotes,
        List<String> diagnosisCodes,
        String payerContextId,

        LocalDateTime preferredAt,
        List<String> flags,

        Long invoiceId
) {
}