package com.plasmit.diagnostic.integration.integration.dto.request;

import java.time.LocalDateTime;

public record RetryExchangeEventRequest(
        String retryReason,
        LocalDateTime nextRetryAt
) {
}