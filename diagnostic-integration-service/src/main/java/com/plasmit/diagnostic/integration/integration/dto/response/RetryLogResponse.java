package com.plasmit.diagnostic.integration.integration.dto.response;

import java.time.LocalDateTime;

public record RetryLogResponse(
        Long retryLogId,
        Long exchangeEventId,
        Integer retryNo,
        String retryStatus,
        String retryReason,
        LocalDateTime attemptedAt,
        LocalDateTime nextRetryAt,
        Long actorUserId
) {
}