package com.plasmit.radiology.hospital.imageshare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeImageShareRequest(

        @NotBlank(message = "reason is required.")
        @Size(max = 500, message = "reason cannot exceed 500 characters.")
        String reason
) {
}