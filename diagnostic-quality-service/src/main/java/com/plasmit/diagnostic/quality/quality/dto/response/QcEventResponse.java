package com.plasmit.diagnostic.quality.quality.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record QcEventResponse(
        Long qcEventId,
        Long equipmentId,
        String qcNo,
        String qcType,
        String department,
        String modality,
        String qcStatus,
        LocalDateTime performedAt,
        String performedBy,
        String remarks,
        LocalDateTime createdAt,
        List<QcResultResponse> results
) {
}