package com.plasmit.diagnostic.report.artifact.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateReportTemplateRequest(
        @NotBlank(message = "templateCode is required.")
        String templateCode,

        @NotBlank(message = "templateName is required.")
        String templateName,

        @NotBlank(message = "department is required.")
        String department,

        String modality,

        @NotBlank(message = "templateBody is required.")
        String templateBody,

        String headerHtml,
        String footerHtml
) {
}