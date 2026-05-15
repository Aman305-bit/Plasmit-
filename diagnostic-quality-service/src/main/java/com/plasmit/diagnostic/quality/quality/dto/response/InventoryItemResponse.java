package com.plasmit.diagnostic.quality.quality.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InventoryItemResponse(
        Long itemId,
        String itemCode,
        String itemName,
        String itemType,
        String department,
        String unit,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        BigDecimal reorderLevel,
        String batchNo,
        LocalDate expiryDate,
        String status,
        Boolean lowStock,
        LocalDateTime createdAt
) {
}