package com.plasmit.diagnostic.integration.integration.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExchangeEventRequest(

        @NotNull(message = "nodeId is required.")
        Long nodeId,

        @NotBlank(message = "eventType is required.")
        String eventType,

        @NotBlank(message = "direction is required.")
        String direction,

        String referenceType,
        Long referenceId,

        String contentType,
        String rawPayload,
        String parsedPayload
) {
}