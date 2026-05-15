package com.plasmit.diagnostic.quality.quality.dto.response;

import java.time.LocalDateTime;

public record CalibrationResponse(
        Long calibrationId,
        Long equipmentId,
        String calibrationStatus,
        LocalDateTime calibratedAt,
        LocalDateTime nextDueAt,
        String calibratedBy,
        String certificateNo,
        String remarks
) {
}