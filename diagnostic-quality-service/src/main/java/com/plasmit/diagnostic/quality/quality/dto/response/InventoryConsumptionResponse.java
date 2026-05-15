package com.plasmit.diagnostic.quality.quality.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryConsumptionResponse(
        Long consumptionId,
        Long itemId,
        Long diagnosticOrderId,
        Long diagnosticOrderLineId,
        Long serviceId,
        BigDecimal quantityConsumed,
        String consumedFor,
        Long consumedBy,
        String remarks,
        LocalDateTime consumedAt
) {
}