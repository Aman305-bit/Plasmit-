package com.plasmit.diagnostic.report.artifact.dto.response;

import java.time.LocalDateTime;

public record QrTokenResponse(
        Long tokenId,
        Long reportId,
        Long artifactId,
        String qrToken,
        String verificationUrl,
        String status,
        LocalDateTime expiresAt,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime revokedAt,
        Long revokedBy,
        String revokeReason
) {
}