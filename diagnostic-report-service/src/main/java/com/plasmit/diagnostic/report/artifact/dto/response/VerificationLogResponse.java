package com.plasmit.diagnostic.report.artifact.dto.response;

import java.time.LocalDateTime;

public record VerificationLogResponse(
        Long id,
        Long reportId,
        Long qrTokenId,
        String verificationStatus,
        String verifiedBy,
        String ipAddress,
        String userAgent,
        String requestId,
        LocalDateTime createdAt
) {
}