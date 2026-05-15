package com.plasmit.diagnostic.governance.governance.validator;

import com.plasmit.diagnostic.governance.common.exception.ApiException;
import com.plasmit.diagnostic.governance.governance.dto.request.*;
import com.plasmit.diagnostic.governance.validator.DateRangeValidator;
import com.plasmit.diagnostic.governance.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class DiagnosticGovernanceValidator {

    private static final Set<String> STATUSES = Set.of("Active", "Inactive");
    private static final Set<String> ACTION_STATUSES = Set.of("Success", "Failed");
    private static final Set<String> ACCESS_STATUSES = Set.of("Granted", "Revoked");

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public DiagnosticGovernanceValidator(DateRangeValidator dateRangeValidator,
                                         PaginationValidator paginationValidator) {
        this.dateRangeValidator = dateRangeValidator;
        this.paginationValidator = paginationValidator;
    }

    public void validateList(LocalDate fromDate,
                             LocalDate toDate,
                             Integer page,
                             Integer limit) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
        paginationValidator.validate(page, limit);
    }

    public void validateId(Long id, String name) {
        if (id == null || id <= 0) {
            throw ApiException.validation("Valid " + name + " is required.");
        }
    }

    public void validateAudit(CreateAuditEventRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.moduleName() == null || request.moduleName().isBlank()) {
            throw ApiException.validation("moduleName is required.");
        }

        if (request.entityType() == null || request.entityType().isBlank()) {
            throw ApiException.validation("entityType is required.");
        }

        if (request.action() == null || request.action().isBlank()) {
            throw ApiException.validation("action is required.");
        }

        if (request.actionStatus() != null
                && !request.actionStatus().isBlank()
                && !ACTION_STATUSES.contains(request.actionStatus())) {
            throw ApiException.validation("Invalid actionStatus.");
        }
    }

    public void validateCreateRole(CreateRoleRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.roleCode() == null || request.roleCode().isBlank()) {
            throw ApiException.validation("roleCode is required.");
        }

        if (request.roleName() == null || request.roleName().isBlank()) {
            throw ApiException.validation("roleName is required.");
        }
    }

    public void validateCreatePermission(CreatePermissionRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.permissionCode() == null || request.permissionCode().isBlank()) {
            throw ApiException.validation("permissionCode is required.");
        }

        if (request.permissionName() == null || request.permissionName().isBlank()) {
            throw ApiException.validation("permissionName is required.");
        }

        if (request.moduleName() == null || request.moduleName().isBlank()) {
            throw ApiException.validation("moduleName is required.");
        }
    }

    public void validateGrant(GrantPermissionRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        validateId(request.permissionId(), "permissionId");
    }

    public void validateRevoke(RevokePermissionRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw ApiException.validation("reason is required.");
        }
    }

    public void validatePolicyRule(CreatePolicyRuleRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.ruleCode() == null || request.ruleCode().isBlank()) {
            throw ApiException.validation("ruleCode is required.");
        }

        if (request.ruleName() == null || request.ruleName().isBlank()) {
            throw ApiException.validation("ruleName is required.");
        }

        if (request.moduleName() == null || request.moduleName().isBlank()) {
            throw ApiException.validation("moduleName is required.");
        }

        if (request.ruleType() == null || request.ruleType().isBlank()) {
            throw ApiException.validation("ruleType is required.");
        }
    }

    public void validatePolicyStatus(UpdatePolicyRuleStatusRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.status() == null || request.status().isBlank()) {
            throw ApiException.validation("status is required.");
        }

        if (!STATUSES.contains(request.status())) {
            throw ApiException.validation("status must be Active or Inactive.");
        }
    }

    public void validateAccessStatus(String status) {
        if (status != null && !status.isBlank() && !ACCESS_STATUSES.contains(status)) {
            throw ApiException.validation("Invalid accessStatus.");
        }
    }
}