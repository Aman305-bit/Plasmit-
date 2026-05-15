package com.plasmit.diagnostics.payment.billingdiagnostics.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DiagnosticCartItemRequest(
        @NotNull(message = "serviceId is required.")
        Long serviceId,

        @NotNull(message = "quantity is required.")
        @Min(value = 1, message = "quantity must be at least 1.")
        Integer quantity,

        Long discountPaise
) {
}