package com.plasmit.diagnostic.governance.governance.dto.response;

import java.time.LocalDateTime;

public record RolePermissionHistoryResponse(
        Long id,
        Long roleId,
        Long permissionId,
        String fromStatus,
        String toStatus,
        String changeReason,
        Long changedBy,
        String requestId,
        LocalDateTime changedAt
) {
}