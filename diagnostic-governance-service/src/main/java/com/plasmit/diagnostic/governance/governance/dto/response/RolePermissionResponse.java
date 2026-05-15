package com.plasmit.diagnostic.governance.governance.dto.response;

import java.time.LocalDateTime;

public record RolePermissionResponse(
        Long id,
        Long roleId,
        String roleCode,
        String roleName,
        Long permissionId,
        String permissionCode,
        String permissionName,
        String moduleName,
        String accessStatus,
        Long grantedBy,
        LocalDateTime grantedAt,
        Long revokedBy,
        LocalDateTime revokedAt,
        String revokeReason
) {
}