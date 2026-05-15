package com.plasmit.diagnostic.governance.governance.dto.response;

import java.time.LocalDateTime;

public record PermissionResponse(
        Long permissionId,
        String permissionCode,
        String permissionName,
        String moduleName,
        String permissionDescription,
        String status,
        Long createdBy,
        LocalDateTime createdAt
) {
}