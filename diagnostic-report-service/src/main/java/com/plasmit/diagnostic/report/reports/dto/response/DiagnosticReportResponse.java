package com.plasmit.diagnostic.report.reports.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record DiagnosticReportResponse(
        Long reportId,
        String reportNo,

        String sourceType,
        Long sourceId,

        Long diagnosticOrderId,
        Long diagnosticOrderLineId,

        Long patientId,
        String patientUhid,
        String patientName,

        String department,
        String modality,

        Long serviceId,
        String serviceCode,
        String serviceName,

        String reportStatus,

        String clinicalSummary,
        String impression,
        String recommendation,

        Boolean abnormalFlag,
        Boolean criticalFlag,

        Boolean expertReviewRequired,
        String expertReviewStatus,

        LocalDateTime signedAt,
        Long signedBy,
        String signedByName,

        LocalDateTime releasedAt,
        Long releasedBy,

        Long createdBy,
        LocalDateTime createdAt,

        List<ReportSectionResponse> sections,
        List<ReportObservationResponse> observations
) {
}