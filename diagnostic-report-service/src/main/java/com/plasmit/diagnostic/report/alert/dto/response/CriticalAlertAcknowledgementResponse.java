package com.plasmit.diagnostic.report.alert.dto.response;

import java.time.LocalDateTime;

public record CriticalAlertAcknowledgementResponse(
        Long alertId,
        Long reportId,
        Long acknowledgedBy,
        String acknowledgedByName,
        String acknowledgementNotes,
        LocalDateTime acknowledgedAt
) {
}