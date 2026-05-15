package com.plasmit.diagnostic.governance.governance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionRequest(
        @NotBlank(message = "permissionCode is required.")
        String permissionCode,

        @NotBlank(message = "permissionName is required.")
        String permissionName,

        @NotBlank(message = "moduleName is required.")
        String moduleName,

        String permissionDescription
) {
}