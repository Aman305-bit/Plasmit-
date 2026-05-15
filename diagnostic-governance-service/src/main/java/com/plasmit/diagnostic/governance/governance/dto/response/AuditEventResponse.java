package com.plasmit.diagnostic.governance.governance.dto.response;

import java.time.LocalDateTime;

public record AuditEventResponse(
        Long auditId,
        String eventCode,
        String moduleName,
        String entityType,
        Long entityId,
        String action,
        String actionStatus,
        String oldValue,
        String newValue,
        String description,
        Long actorUserId,
        String actorName,
        String actorRole,
        String ipAddress,
        String userAgent,
        String requestId,
        LocalDateTime createdAt
) {
}