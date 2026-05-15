package com.plasmit.diagnostic.quality.quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateCalibrationRequest(
        @NotNull(message = "equipmentId is required.")
        Long equipmentId,

        @NotBlank(message = "calibrationStatus is required.")
        String calibrationStatus,

        @NotNull(message = "calibratedAt is required.")
        LocalDateTime calibratedAt,

        LocalDateTime nextDueAt,
        String calibratedBy,
        String certificateNo,
        String remarks
) {
}