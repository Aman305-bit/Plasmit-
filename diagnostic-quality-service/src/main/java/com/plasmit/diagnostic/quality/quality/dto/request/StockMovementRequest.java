package com.plasmit.diagnostic.quality.quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockMovementRequest(
        @NotNull(message = "itemId is required.")
        Long itemId,

        @NotBlank(message = "movementType is required.")
        String movementType,

        @NotNull(message = "quantity is required.")
        BigDecimal quantity,

        String referenceType,
        Long referenceId,
        String remarks
) {
}