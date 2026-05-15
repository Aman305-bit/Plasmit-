package com.plasmit.diagnostic.quality.quality.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementResponse(
        Long movementId,
        Long itemId,
        String movementType,
        BigDecimal quantity,
        BigDecimal stockBefore,
        BigDecimal stockAfter,
        String referenceType,
        Long referenceId,
        String remarks,
        LocalDateTime createdAt
) {
}