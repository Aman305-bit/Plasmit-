package com.plasmit.diagnostic.report.reports.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AmendReportRequest(
        @NotBlank(message = "amendmentReason is required.")
        String amendmentReason,

        @NotBlank(message = "amendedContent is required.")
        String amendedContent
) {
}