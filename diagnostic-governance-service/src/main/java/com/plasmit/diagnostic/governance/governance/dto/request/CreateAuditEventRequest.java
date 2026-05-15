package com.plasmit.diagnostic.governance.governance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateAuditEventRequest(
        @NotBlank(message = "moduleName is required.")
        String moduleName,

        @NotBlank(message = "entityType is required.")
        String entityType,

        Long entityId,

        @NotBlank(message = "action is required.")
        String action,

        String actionStatus,
        String oldValue,
        String newValue,
        String description,

        String actorName,
        String ipAddress,
        String userAgent
) {
}