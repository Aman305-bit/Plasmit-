package com.plasmit.diagnostic.report.reports.dto.response;

public record ReportSectionResponse(
        Long id,
        String sectionTitle,
        String sectionContent,
        Integer displayOrder
) {
}