package com.plasmit.diagnostics.payment.diagnostics.dto.response;

import java.util.List;

public record DiagnosticsBootstrapResponse(
        List<String> branches,
        List<String> departments,
        List<String> modalities,
        List<String> priorities,
        List<String> statuses,
        List<String> sources
) {
}