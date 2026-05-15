package com.plasmit.diagnostic.quality.quality.dto.response;

import java.time.LocalDateTime;

public record MaintenanceResponse(
        Long maintenanceId,
        Long equipmentId,
        String maintenanceType,
        String maintenanceStatus,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String performedBy,
        String vendorTicketNo,
        String issueDescription,
        String resolutionNotes,
        LocalDateTime createdAt
) {
}