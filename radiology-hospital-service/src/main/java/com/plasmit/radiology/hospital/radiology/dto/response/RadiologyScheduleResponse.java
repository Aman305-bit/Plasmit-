package com.plasmit.radiology.hospital.radiology.dto.response;

import java.time.LocalDateTime;

public record RadiologyScheduleResponse(
        Long studyId,
        String status,
        LocalDateTime scheduledAt,
        String scheduledRoom,
        Long radiologistId,
        String radiologistName
) {
}