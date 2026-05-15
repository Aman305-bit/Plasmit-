package com.plasmit.diagnostic.governance.governance.dto.response;

import java.time.LocalDateTime;

public record PolicyRuleResponse(
        Long ruleId,
        String ruleCode,
        String ruleName,
        String moduleName,
        String ruleType,
        String ruleConfig,
        String ruleStatus,
        Long createdBy,
        LocalDateTime createdAt
) {
}