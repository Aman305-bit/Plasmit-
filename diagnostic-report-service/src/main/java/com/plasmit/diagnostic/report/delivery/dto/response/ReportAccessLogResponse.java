package com.plasmit.diagnostic.report.delivery.dto.response;

import java.time.LocalDateTime;

public record ReportAccessLogResponse(
        Long id,
        Long reportId,
        Long accessTokenId,
        String accessType,
        String accessedBy,
        String ipAddress,
        String userAgent,
        String requestId,
        LocalDateTime createdAt
) {
}