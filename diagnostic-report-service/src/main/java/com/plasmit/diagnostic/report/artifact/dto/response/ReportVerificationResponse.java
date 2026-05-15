package com.plasmit.diagnostic.report.artifact.dto.response;

import com.plasmit.diagnostic.report.reports.dto.response.DiagnosticReportResponse;

public record ReportVerificationResponse(
        String verificationStatus,
        Long tokenId,
        Long reportId,
        String reportNo,
        String patientName,
        String reportStatus,
        String artifactUrl,
        DiagnosticReportResponse report
) {
}