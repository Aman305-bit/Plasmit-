package com.plasmit.diagnostics.payment.diagnostics.dto.response;

public record DiagnosticsSummaryResponse(
        Long openOrders,
        Long radiologyStudies,
        Long pathologySpecimens,
        Long tatBreaches,
        Long criticalAlertsPending,
        Long reportsPendingSignoff,
        Long integrationQueue,
        Long qualityBlocked
) {
}