package com.plasmit.diagnostic.governance.governance.dto.response;

import java.time.LocalDateTime;

public record RoleResponse(
        Long roleId,
        String roleCode,
        String roleName,
        String roleDescription,
        String status,
        Long createdBy,
        LocalDateTime createdAt
) {
}