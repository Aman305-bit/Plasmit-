package com.plasmit.diagnostics.payment.diagnostics.dto.response;

import java.time.LocalDateTime;

public record BillingAuthorizationResponse(
        Long orderId,
        String action,
        String authorizationStatus,
        Boolean releaseBlocked,
        String reason,
        LocalDateTime createdAt
) {
}