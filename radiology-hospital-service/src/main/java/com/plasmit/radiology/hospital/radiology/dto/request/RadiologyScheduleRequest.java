package com.plasmit.radiology.hospital.radiology.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RadiologyScheduleRequest(
        @NotNull(message = "scheduledAt is required.")
        LocalDateTime scheduledAt,

        @NotBlank(message = "scheduledRoom is required.")
        String scheduledRoom,

        Long radiologistId,
        String radiologistName,
        String notes
) {
}