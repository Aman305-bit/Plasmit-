package com.plasmit.diagnostic.integration.integration.dto.response;

import java.time.LocalDateTime;

public record ExchangePayloadResponse(
        Long payloadId,
        Long exchangeEventId,
        String payloadType,
        String contentType,
        String rawPayload,
        String parsedPayload,
        String checksum,
        LocalDateTime createdAt
) {
}