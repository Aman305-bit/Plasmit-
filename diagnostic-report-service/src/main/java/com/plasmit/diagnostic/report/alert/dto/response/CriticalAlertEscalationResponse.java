package com.plasmit.diagnostic.report.alert.dto.response;

import java.time.LocalDateTime;

public record CriticalAlertEscalationResponse(
        Long alertId,
        Long reportId,
        Long escalatedFromUserId,
        Long escalatedToUserId,
        String escalatedToName,
        String escalatedToRole,
        String escalationReason,
        Integer escalationLevel,
        LocalDateTime escalatedAt
) {
}