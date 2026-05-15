package com.plasmit.diagnostic.report.alert.dto.response;

import java.time.LocalDateTime;

public record CriticalAlertEventResponse(
        Long id,
        Long alertId,
        Long reportId,
        String fromStatus,
        String toStatus,
        String eventType,
        String reason,
        String notes,
        Long actorUserId,
        String actorRole,
        String requestId,
        LocalDateTime createdAt
) {
}