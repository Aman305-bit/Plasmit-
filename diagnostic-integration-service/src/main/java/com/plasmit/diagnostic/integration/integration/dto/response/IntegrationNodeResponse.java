package com.plasmit.diagnostic.integration.integration.dto.response;

import java.time.LocalDateTime;

public record IntegrationNodeResponse(
        Long nodeId,
        String nodeCode,
        String nodeName,
        String integrationType,
        String direction,
        String endpointUrl,
        String protocol,
        String authType,
        String nodeStatus,
        LocalDateTime lastHeartbeatAt,
        Integer failureCount,
        LocalDateTime createdAt
) {
}