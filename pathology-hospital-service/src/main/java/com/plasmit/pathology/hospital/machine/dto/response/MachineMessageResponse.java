package com.plasmit.pathology.hospital.machine.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record MachineMessageResponse(
        Long messageId,
        Long machineId,
        String messageControlId,
        String machineSampleId,
        String barcodeNo,
        String patientUhid,
        String patientName,
        Long matchedSpecimenId,
        String matchStatus,
        String processingStatus,
        String rejectionReason,
        LocalDateTime receivedAt,
        LocalDateTime processedAt,
        List<MachineResultParameterResponse> parameters
) {
}