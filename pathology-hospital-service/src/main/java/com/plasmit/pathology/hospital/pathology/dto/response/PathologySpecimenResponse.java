package com.plasmit.pathology.hospital.pathology.dto.response;

import java.time.LocalDateTime;

public record PathologySpecimenResponse(
        Long specimenId,
        Long diagnosticOrderId,
        Long diagnosticOrderLineId,
        Long serviceId,
        String specimenNo,
        String barcodeNo,
        Long patientId,
        String patientUhid,
        String patientName,
        String serviceCode,
        String serviceName,
        String modality,
        String sampleType,
        String priority,
        String status,
        String resultStatus,
        LocalDateTime collectedAt,
        LocalDateTime receivedAt,
        LocalDateTime rejectedAt,
        String rejectedReason,
        LocalDateTime technicalVerifiedAt,
        LocalDateTime clinicalVerifiedAt,
        LocalDateTime createdAt
) {
}