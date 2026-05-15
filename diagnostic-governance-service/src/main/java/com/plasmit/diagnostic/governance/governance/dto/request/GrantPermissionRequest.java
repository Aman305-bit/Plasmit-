package com.plasmit.diagnostic.governance.governance.dto.request;

import jakarta.validation.constraints.NotNull;

public record GrantPermissionRequest(
        @NotNull(message = "permissionId is required.")
        Long permissionId,

        String reason
) {
}