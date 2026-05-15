package com.plasmit.diagnostic.report.artifact.dto.request;

import java.time.LocalDateTime;

public record CreateQrTokenRequest(
        Long artifactId,
        LocalDateTime expiresAt
) {
}