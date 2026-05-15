package com.plasmit.diagnostic.quality.quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateMaintenanceRequest(
        @NotNull(message = "equipmentId is required.")
        Long equipmentId,

        @NotBlank(message = "maintenanceType is required.")
        String maintenanceType,

        LocalDateTime scheduledAt,
        String performedBy,
        String vendorTicketNo,
        String issueDescription
) {
}