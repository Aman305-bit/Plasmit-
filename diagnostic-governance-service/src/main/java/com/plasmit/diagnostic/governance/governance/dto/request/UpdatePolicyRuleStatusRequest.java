package com.plasmit.diagnostic.governance.governance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdatePolicyRuleStatusRequest(
        @NotBlank(message = "status is required.")
        String status,

        String reason
) {
}