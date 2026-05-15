package com.plasmit.diagnostic.report.reports.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExpertReviewRequest(
        @NotBlank(message = "reviewerName is required.")
        String reviewerName,

        @NotBlank(message = "reviewStatus is required.")
        String reviewStatus,

        String comments
) {
}