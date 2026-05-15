package com.plasmit.diagnostic.report.reports.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignReportRequest(
        @NotBlank(message = "signerName is required.")
        String signerName,

        String signerRole
) {
}