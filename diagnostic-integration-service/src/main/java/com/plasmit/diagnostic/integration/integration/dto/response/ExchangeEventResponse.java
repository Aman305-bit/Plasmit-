package com.plasmit.diagnostic.integration.integration.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ExchangeEventResponse(
        Long eventId,
        Long nodeId,
        String eventCode,
        String eventType,
        String direction,
        String referenceType,
        Long referenceId,
        String exchangeStatus,
        Integer retryCount,
        Integer maxRetryCount,
        String errorMessage,
        LocalDateTime receivedAt,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        List<ExchangePayloadResponse> payloads
) {
}