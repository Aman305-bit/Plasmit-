package com.plasmit.diagnostic.report.reports.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReleaseReportRequest(
        @NotBlank(message = "releaseChannel is required.")
        String releaseChannel,

        String remarks
) {
}