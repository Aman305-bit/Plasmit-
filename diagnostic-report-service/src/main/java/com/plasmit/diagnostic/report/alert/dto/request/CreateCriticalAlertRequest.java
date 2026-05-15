package com.plasmit.diagnostic.report.alert.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record CreateCriticalAlertRequest(

        @NotBlank(message = "alertMessage is required.")
        String alertMessage,

        String severity,

        Long notifiedToUserId,
        String notifiedToName,
        String notifiedToRole,
        String notifiedToMobile,
        String notifiedToEmail,

        LocalDateTime dueAt
) {
}