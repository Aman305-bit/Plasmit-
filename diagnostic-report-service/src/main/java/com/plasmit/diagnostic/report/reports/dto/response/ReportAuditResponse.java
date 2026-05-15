package com.plasmit.diagnostic.report.reports.dto.response;

import java.time.LocalDateTime;

public record ReportAuditResponse(
        Long id,
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