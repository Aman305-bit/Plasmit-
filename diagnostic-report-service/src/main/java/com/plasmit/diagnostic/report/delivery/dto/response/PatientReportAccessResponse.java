package com.plasmit.diagnostic.report.delivery.dto.response;

import com.plasmit.diagnostic.report.reports.dto.response.DiagnosticReportResponse;

public record PatientReportAccessResponse(
        Long tokenId,
        String accessStatus,
        DiagnosticReportResponse report
) {
}