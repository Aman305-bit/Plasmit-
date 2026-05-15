package com.plasmit.pathology.hospital.machine.dto.request;

import jakarta.validation.constraints.NotNull;

public record MachineMessageMatchRequest(
        @NotNull(message = "specimenId is required.")
        Long specimenId,

        String notes
) {
}