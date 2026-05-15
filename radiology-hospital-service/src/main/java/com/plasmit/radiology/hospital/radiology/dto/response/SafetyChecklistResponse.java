package com.plasmit.radiology.hospital.radiology.dto.response;

public record SafetyChecklistResponse(
        Long studyId,
        String status,
        Boolean consentTaken
) {
}