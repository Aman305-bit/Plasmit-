package com.plasmit.diagnostic.report.reports.dto.response;

import java.time.LocalDateTime;

public record ReportReleaseResponse(
        Long reportId,
        String releaseChannel,
        Long releasedBy,
        String remarks,
        LocalDateTime releasedAt
) {
}