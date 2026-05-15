package com.plasmit.pathology.hospital.pathology.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ResultEntryResponse(
        Long resultEntryId,
        Long specimenId,
        String resultNo,
        String resultStatus,
        String resultSummary,
        Boolean abnormalFlag,
        Boolean criticalFlag,
        Long enteredBy,
        LocalDateTime enteredAt,
        List<ResultParameterResponse> parameters
) {
}