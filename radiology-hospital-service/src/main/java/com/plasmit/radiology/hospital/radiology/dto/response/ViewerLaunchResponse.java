package com.plasmit.radiology.hospital.radiology.dto.response;

public record ViewerLaunchResponse(
        Long studyId,
        String studyUid,
        String viewerUrl,
        String expiresIn
) {
}