package com.plasmit.diagnostic.quality.quality.dto.response;

import java.time.LocalDateTime;

public record EquipmentResponse(
        Long equipmentId,
        String equipmentCode,
        String equipmentName,
        String equipmentType,
        String department,
        String modality,
        String vendorName,
        String modelName,
        String serialNo,
        String location,
        String operationalStatus,
        LocalDateTime lastCalibratedAt,
        LocalDateTime nextCalibrationDueAt,
        Boolean active,
        LocalDateTime createdAt
) {
}