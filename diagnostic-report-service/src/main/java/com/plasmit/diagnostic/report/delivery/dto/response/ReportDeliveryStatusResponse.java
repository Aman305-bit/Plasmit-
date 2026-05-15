package com.plasmit.diagnostic.report.delivery.dto.response;

import java.util.List;

public record ReportDeliveryStatusResponse(
        Long reportId,
        String reportStatus,
        List<ReportDeliveryResponse> deliveries,
        List<ReportAccessTokenResponse> accessTokens
) {
}