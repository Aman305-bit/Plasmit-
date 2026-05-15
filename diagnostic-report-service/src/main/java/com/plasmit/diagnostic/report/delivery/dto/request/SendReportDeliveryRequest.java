package com.plasmit.diagnostic.report.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SendReportDeliveryRequest(
        @NotBlank(message = "deliveryChannel is required.")
        String deliveryChannel,

        @NotBlank(message = "recipientType is required.")
        String recipientType,

        String recipientName,
        String recipientMobile,
        String recipientEmail,

        String remarks
) {
}