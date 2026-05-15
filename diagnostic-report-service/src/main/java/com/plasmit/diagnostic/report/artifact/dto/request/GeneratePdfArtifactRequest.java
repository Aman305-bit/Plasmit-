package com.plasmit.diagnostic.report.artifact.dto.request;

public record GeneratePdfArtifactRequest(
        Long versionId,
        String filePath,
        String fileUrl
) {
}