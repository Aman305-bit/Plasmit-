package com.plasmit.diagnostics.payment.billingdiagnostics.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DiagnosticCartValidateRequest(
        @NotBlank(message = "payerType is required.")
        String payerType,

        @Valid
        @NotEmpty(message = "items are required.")
        List<DiagnosticCartItemRequest> items
) {
}