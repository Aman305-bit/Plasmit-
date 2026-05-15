package com.plasmit.diagnostic.report.alert.dto.response;

import java.time.LocalDateTime;

public record CriticalAlertResponse(
        Long alertId,
        Long reportId,
        String alertNo,

        Long patientId,
        String patientUhid,
        String patientName,

        String department,
        String modality,
        String serviceName,

        String severity,
        String alertStatus,
        String alertMessage,

        Long notifiedToUserId,
        String notifiedToName,
        String notifiedToRole,
        String notifiedToMobile,
        String notifiedToEmail,

        LocalDateTime dueAt,

        LocalDateTime acknowledgedAt,
        Long acknowledgedBy,
        String acknowledgedByName,
        String acknowledgementNotes,

        LocalDateTime escalatedAt,
        Long escalatedToUserId,
        String escalatedToName,
        String escalationReason,

        LocalDateTime closedAt,
        Long closedBy,
        String closeNotes,

        Long createdBy,
        LocalDateTime createdAt
) {
}