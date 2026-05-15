package com.plasmit.pathology.hospital.machine.dto.response;

import java.time.LocalDateTime;

public record PathologyMachineResponse(
        Long machineId,
        String machineCode,
        String machineName,
        String vendorName,
        String modelName,
        String department,
        String modality,
        String connectionType,
        String interfaceStatus,
        String location,
        Boolean active,
        LocalDateTime createdAt
) {
}