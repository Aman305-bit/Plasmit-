package com.plasmit.diagnostic.report.reports.dto.response;

import java.time.LocalDateTime;

public record ReportAmendmentResponse(
        Long reportId,
        String amendmentNo,
        String amendmentReason,
        String amendedContent,
        Long amendedBy,
        LocalDateTime amendedAt
) {
}