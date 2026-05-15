package com.plasmit.diagnostic.quality.quality.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InventoryConsumptionRequest(
        @NotNull(message = "itemId is required.")
        Long itemId,

        Long diagnosticOrderId,
        Long diagnosticOrderLineId,
        Long serviceId,

        @NotNull(message = "quantityConsumed is required.")
        BigDecimal quantityConsumed,

        String consumedFor,
        String remarks
) {
}