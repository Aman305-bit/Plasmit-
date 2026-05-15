package com.plasmit.diagnostic.report.reports.dto.response;

import java.time.LocalDateTime;

public record ReportSignatureResponse(
        Long reportId,
        Long signerUserId,
        String signerName,
        String signerRole,
        String signatureHash,
        LocalDateTime signedAt
) {
}