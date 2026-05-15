package com.plasmit.pathology.hospital.pathology.dto.response;

import java.time.LocalDateTime;

public record VerificationResponse(
        Long specimenId,
        Long resultEntryId,
        String verificationType,
        String verificationStatus,
        Long verifierUserId,
        String verifierName,
        String remarks,
        LocalDateTime verifiedAt
) {
}