package com.plasmit.pathology.hospital.machine.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MachineMessageParameterRequest(
        @NotBlank(message = "machineTestCode is required.")
        String machineTestCode,

        String machineTestName,

        @NotBlank(message = "resultValue is required.")
        String resultValue,

        String unit,
        Boolean abnormalFlag,
        Boolean criticalFlag,
        Integer displayOrder
) {
}