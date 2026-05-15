package com.plasmit.radiology.hospital.imageshare.dto.response;

import java.time.LocalDateTime;

public record ImageShareAccessLogResponse(
        Long id,
        Long shareId,
        Long studyId,
        String accessType,
        String accessedBy,
        String ipAddress,
        String userAgent,
        String requestId,
        LocalDateTime createdAt
) {
}