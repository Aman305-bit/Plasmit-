package com.plasmit.diagnostic.integration.integration.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FailExchangeEventRequest(

        @NotBlank(message = "errorMessage is required.")
        String errorMessage
) {
}