package com.plasmit.diagnostic.report.artifact.dto.response;

import java.time.LocalDateTime;

public record PdfArtifactResponse(
        Long artifactId,
        Long reportId,
        Long versionId,
        String artifactNo,
        String artifactType,
        String fileName,
        String filePath,
        String fileUrl,
        String contentHash,
        String generationStatus,
        Long generatedBy,
        LocalDateTime generatedAt
) {
}