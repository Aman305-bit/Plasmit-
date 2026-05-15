package com.plasmit.diagnostic.governance.governance.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePolicyRuleRequest(
        @NotBlank(message = "ruleCode is required.")
        String ruleCode,

        @NotBlank(message = "ruleName is required.")
        String ruleName,

        @NotBlank(message = "moduleName is required.")
        String moduleName,

        @NotBlank(message = "ruleType is required.")
        String ruleType,

        String ruleConfig
) {
}