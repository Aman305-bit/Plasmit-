package com.plasmit.diagnostic.report.reports.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReportSectionRequest(
        @NotBlank(message = "sectionTitle is required.")
        String sectionTitle,

        String sectionContent,

        Integer displayOrder
) {
}