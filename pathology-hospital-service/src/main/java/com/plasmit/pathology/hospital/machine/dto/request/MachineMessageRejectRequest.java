package com.plasmit.pathology.hospital.machine.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MachineMessageRejectRequest(
        @NotBlank(message = "reason is required.")
        String reason
) {
}