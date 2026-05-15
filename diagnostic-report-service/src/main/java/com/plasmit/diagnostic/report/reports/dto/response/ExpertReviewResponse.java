package com.plasmit.diagnostic.report.reports.dto.response;

import java.time.LocalDateTime;

public record ExpertReviewResponse(
        Long reportId,
        Long reviewerUserId,
        String reviewerName,
        String reviewStatus,
        String comments,
        LocalDateTime reviewedAt
) {
}