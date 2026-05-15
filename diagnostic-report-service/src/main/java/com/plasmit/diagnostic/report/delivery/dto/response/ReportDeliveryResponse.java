package com.plasmit.diagnostic.report.delivery.dto.response;

import java.time.LocalDateTime;

public record ReportDeliveryResponse(
        Long deliveryId,
        Long reportId,
        String deliveryChannel,
        String recipientType,
        String recipientName,
        String recipientMobile,
        String recipientEmail,
        String deliveryStatus,
        String deliveryReference,
        String failureReason,
        Long sentBy,
        LocalDateTime sentAt
) {
}