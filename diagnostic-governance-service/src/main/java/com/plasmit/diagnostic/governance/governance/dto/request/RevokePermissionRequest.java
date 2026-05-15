package com.plasmit.diagnostic.governance.governance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RevokePermissionRequest(
        @NotBlank(message = "reason is required.")
        String reason
) {
}