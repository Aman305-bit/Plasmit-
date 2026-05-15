package com.plasmit.diagnostic.report.delivery.dto.response;

import java.time.LocalDateTime;

public record ReportAccessTokenResponse(
        Long tokenId,
        Long reportId,
        String accessToken,
        String accessUrl,
        String recipientType,
        String recipientName,
        String recipientMobile,
        String recipientEmail,
        LocalDateTime expiresAt,
        String status,
        LocalDateTime revokedAt,
        Long revokedBy,
        String revokeReason,
        Long createdBy,
        LocalDateTime createdAt
) {
}