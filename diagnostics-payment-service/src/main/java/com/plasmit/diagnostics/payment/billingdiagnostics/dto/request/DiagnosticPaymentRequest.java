package com.plasmit.diagnostics.payment.billingdiagnostics.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiagnosticPaymentRequest(
        @NotBlank(message = "paymentMethod is required.")
        String paymentMethod,

        @NotNull(message = "amountPaise is required.")
        @Min(value = 0, message = "amountPaise cannot be negative.")
        Long amountPaise,

        String referenceNo
) {
}