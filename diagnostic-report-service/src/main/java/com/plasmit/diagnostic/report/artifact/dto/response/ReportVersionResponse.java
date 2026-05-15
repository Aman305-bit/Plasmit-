package com.plasmit.diagnostic.report.artifact.dto.response;

import java.time.LocalDateTime;

public record ReportVersionResponse(
        Long versionId,
        Long reportId,
        Integer versionNo,
        String versionStatus,
        String versionReason,
        String snapshotJson,
        Long createdBy,
        LocalDateTime createdAt
) {
}