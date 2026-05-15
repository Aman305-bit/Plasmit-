package com.plasmit.diagnostic.report.reports.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateDiagnosticReportRequest(
        @NotBlank(message = "sourceType is required.")
        String sourceType,

        @NotNull(message = "sourceId is required.")
        Long sourceId,

        Long diagnosticOrderId,
        Long diagnosticOrderLineId,

        @NotNull(message = "patientId is required.")
        Long patientId,

        String patientUhid,

        @NotBlank(message = "patientName is required.")
        String patientName,

        @NotBlank(message = "department is required.")
        String department,

        String modality,

        Long serviceId,
        String serviceCode,
        String serviceName,

        String clinicalSummary,
        String impression,
        String recommendation,

        Boolean abnormalFlag,
        Boolean criticalFlag,

        Boolean expertReviewRequired,

        @Valid
        List<ReportSectionRequest> sections,

        @Valid
        List<ReportObservationRequest> observations
) {
}