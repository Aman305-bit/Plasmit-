package com.plasmit.radiology.hospital.radiology.dto.response;

import java.time.LocalDateTime;

public record RadiologyStudyResponse(
        Long studyId,
        Long diagnosticOrderId,
        Long diagnosticOrderLineId,
        Long serviceId,
        String studyUid,
        String accessionNo,
        String modality,
        Long patientId,
        String patientUhid,
        String patientName,
        String priority,
        String status,
        LocalDateTime scheduledAt,
        String scheduledRoom,
        Long radiologistId,
        String radiologistName,
        String pacsStatus,
        String viewerUrl,
        LocalDateTime createdAt
) {
}