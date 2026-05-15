package com.plasmit.diagnostic.governance.governance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
        @NotBlank(message = "roleCode is required.")
        String roleCode,

        @NotBlank(message = "roleName is required.")
        String roleName,

        String roleDescription
) {
}