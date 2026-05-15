package com.plasmit.pathology.hospital.machine.dto.response;

public record MachineResultParameterResponse(
        Long id,
        String machineTestCode,
        String machineTestName,
        String parameterCode,
        String parameterName,
        String resultValue,
        String unit,
        String referenceRange,
        Boolean abnormalFlag,
        Boolean criticalFlag,
        Integer displayOrder
) {
}