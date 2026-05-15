package com.plasmit.diagnostic.report.artifact.dto.response;

import java.time.LocalDateTime;

public record ReportTemplateResponse(
        Long templateId,
        String templateCode,
        String templateName,
        String department,
        String modality,
        String templateBody,
        String headerHtml,
        String footerHtml,
        String status,
        Long createdBy,
        LocalDateTime createdAt
) {
}