package com.plasmit.diagnostics.payment.billingdiagnostics.dto.response;

import java.time.LocalDateTime;

public record DiagnosticHandoffResponse(
        Long id,
        Long invoiceId,
        Long serviceId,
        String serviceName,
        String modality,
        String handoffRoute,
        String handoffStatus,
        Integer attempts,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}