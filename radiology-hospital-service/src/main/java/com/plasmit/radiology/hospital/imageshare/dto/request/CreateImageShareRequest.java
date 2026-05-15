package com.plasmit.radiology.hospital.imageshare.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateImageShareRequest(

        @NotNull(message = "studyId is required.")
        Long studyId,

        @Size(max = 150, message = "recipientName cannot exceed 150 characters.")
        String recipientName,

        @Size(max = 30, message = "recipientMobile cannot exceed 30 characters.")
        String recipientMobile,

        @Size(max = 150, message = "recipientEmail cannot exceed 150 characters.")
        String recipientEmail,

        @NotBlank(message = "sharePurpose is required.")
        String sharePurpose,

        @NotBlank(message = "accessScope is required.")
        String accessScope,

        @NotNull(message = "expiresAt is required.")
        @Future(message = "expiresAt must be in future.")
        LocalDateTime expiresAt
) {
}