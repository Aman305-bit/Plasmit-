package com.plasmit.radiology.hospital.radiology.dto.response;

public record PacsMatchResponse(
        Long studyId,
        String pacsStudyUid,
        String pacsStatus,
        Double matchConfidence
) {
}