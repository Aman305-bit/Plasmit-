package com.plasmit.radiology.hospital.imageshare.dto.response;

import java.time.LocalDateTime;

public record ImageShareResponse(
        Long shareId,
        Long studyId,
        String shareCode,

        String recipientName,
        String recipientMobile,
        String recipientEmail,

        String sharePurpose,
        String accessScope,

        String shareUrl,
        LocalDateTime expiresAt,

        String status,
        LocalDateTime revokedAt,
        Long revokedBy,
        String revokeReason,

        Long createdBy,
        LocalDateTime createdAt
) {
}