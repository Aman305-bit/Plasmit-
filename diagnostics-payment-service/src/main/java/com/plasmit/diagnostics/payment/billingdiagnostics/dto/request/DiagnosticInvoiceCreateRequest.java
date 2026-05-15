package com.plasmit.diagnostics.payment.billingdiagnostics.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DiagnosticInvoiceCreateRequest(
        @NotNull(message = "patientId is required.")
        Long patientId,

        String patientUhid,

        @NotBlank(message = "patientName is required.")
        String patientName,

        String patientAgeGender,

        Long encounterId,
        String encounterType,

        @NotBlank(message = "payerType is required.")
        String payerType,

        String payerName,
        String authorizationNo,

        @Valid
        @NotEmpty(message = "items are required.")
        List<DiagnosticCartItemRequest> items,

        @Valid
        List<DiagnosticPaymentRequest> payments
) {
}