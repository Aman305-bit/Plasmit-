package com.plasmit.diagnostic.integration.integration.dto.response;

import java.time.LocalDateTime;

public record IntegrationAuditResponse(
        Long id,
        String entityType,
        Long entityId,
        String eventType,
        String fromStatus,
        String toStatus,
        String notes,
        Long actorUserId,
        String actorRole,
        String requestId,
        LocalDateTime createdAt
) {
}