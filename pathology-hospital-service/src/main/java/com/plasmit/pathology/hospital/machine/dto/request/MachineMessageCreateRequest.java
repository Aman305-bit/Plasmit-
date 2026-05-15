package com.plasmit.pathology.hospital.machine.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MachineMessageCreateRequest(
        @NotNull(message = "machineId is required.")
        Long machineId,

        String messageControlId,
        String machineSampleId,
        String barcodeNo,

        String patientUhid,
        String patientName,

        String rawMessage,

        @Valid
        @NotEmpty(message = "parameters are required.")
        List<MachineMessageParameterRequest> parameters
) {
}