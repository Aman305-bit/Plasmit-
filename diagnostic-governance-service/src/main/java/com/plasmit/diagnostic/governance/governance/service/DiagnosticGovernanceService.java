package com.plasmit.diagnostic.governance.governance.service;

import com.plasmit.diagnostic.governance.common.response.PageResponse;
import com.plasmit.diagnostic.governance.common.response.PaginationMeta;
import com.plasmit.diagnostic.governance.context.TenantContext;
import com.plasmit.diagnostic.governance.governance.dto.request.*;
import com.plasmit.diagnostic.governance.governance.dto.response.*;
import com.plasmit.diagnostic.governance.governance.repository.DiagnosticGovernanceRepository;
import com.plasmit.diagnostic.governance.governance.validator.DiagnosticGovernanceValidator;
import com.plasmit.diagnostic.governance.validator.CommonRequestValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DiagnosticGovernanceService {

    private final DiagnosticGovernanceRepository repository;
    private final DiagnosticGovernanceValidator validator;
    private final CommonRequestValidator commonRequestValidator;

    public DiagnosticGovernanceService(DiagnosticGovernanceRepository repository,
                                       DiagnosticGovernanceValidator validator,
                                       CommonRequestValidator commonRequestValidator) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
    }

    public PageResponse<AuditEventResponse> getAuditEvents(LocalDate fromDate,
                                                           LocalDate toDate,
                                                           String moduleName,
                                                           String entityType,
                                                           String action,
                                                           String search,
                                                           Integer page,
                                                           Integer limit) {

        validateContext();
        validator.validateList(fromDate, toDate, page, limit);

        Long total = repository.countAuditEvents(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                fromDate,
                toDate,
                moduleName,
                entityType,
                action,
                search
        );

        List<AuditEventResponse> rows = repository.findAuditEvents(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                fromDate,
                toDate,
                moduleName,
                entityType,
                action,
                search,
                page,
                limit
        );

        return new PageResponse<>(
                rows,
                new PaginationMeta(page, limit, total)
        );
    }

    @Transactional
    public AuditEventResponse createAuditEvent(CreateAuditEventRequest request) {
        validateContext();
        validator.validateAudit(request);

        return repository.createAuditEvent(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId(),
                request,
                "AUD-" + System.currentTimeMillis()
        );
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        validateContext();
        validator.validateCreateRole(request);

        RoleResponse response = repository.createRole(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                request
        );

        internalAudit(
                "GOVERNANCE",
                "ROLE",
                response.roleId(),
                "ROLE_CREATED",
                null,
                response.roleCode(),
                "Governance role created."
        );

        return response;
    }

    public List<RoleResponse> getRoles(String status) {
        validateContext();

        return repository.findRoles(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                status
        );
    }

    public RoleResponse getRole(Long roleId) {
        validateContext();
        validator.validateId(roleId, "roleId");

        return repository.findRoleById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                roleId
        );
    }

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        validateContext();
        validator.validateCreatePermission(request);

        PermissionResponse response = repository.createPermission(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                request
        );

        internalAudit(
                "GOVERNANCE",
                "PERMISSION",
                response.permissionId(),
                "PERMISSION_CREATED",
                null,
                response.permissionCode(),
                "Governance permission created."
        );

        return response;
    }

    public List<PermissionResponse> getPermissions(String moduleName, String status) {
        validateContext();

        return repository.findPermissions(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                moduleName,
                status
        );
    }

    public PermissionResponse getPermission(Long permissionId) {
        validateContext();
        validator.validateId(permissionId, "permissionId");

        return repository.findPermissionById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                permissionId
        );
    }

    @Transactional
    public RolePermissionResponse grantPermission(Long roleId,
                                                  GrantPermissionRequest request) {
        validateContext();
        validator.validateId(roleId, "roleId");
        validator.validateGrant(request);

        RolePermissionResponse response = repository.grantPermission(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                roleId,
                request.permissionId(),
                request.reason(),
                TenantContext.getRequestId()
        );

        internalAudit(
                "GOVERNANCE",
                "ROLE_PERMISSION",
                response.id(),
                "PERMISSION_GRANTED",
                null,
                response.accessStatus(),
                "Permission granted to role."
        );

        return response;
    }

    @Transactional
    public RolePermissionResponse revokePermission(Long rolePermissionId,
                                                   RevokePermissionRequest request) {
        validateContext();
        validator.validateId(rolePermissionId, "rolePermissionId");
        validator.validateRevoke(request);

        RolePermissionResponse response = repository.revokePermission(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                rolePermissionId,
                request.reason(),
                TenantContext.getRequestId()
        );

        internalAudit(
                "GOVERNANCE",
                "ROLE_PERMISSION",
                response.id(),
                "PERMISSION_REVOKED",
                "Granted",
                "Revoked",
                request.reason()
        );

        return response;
    }

    public List<RolePermissionResponse> getRolePermissions(Long roleId,
                                                           String accessStatus) {
        validateContext();

        if (roleId != null) {
            validator.validateId(roleId, "roleId");
        }

        validator.validateAccessStatus(accessStatus);

        return repository.findRolePermissions(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                roleId,
                accessStatus
        );
    }

    public List<RolePermissionHistoryResponse> getRolePermissionHistory(Long roleId,
                                                                        Long permissionId) {
        validateContext();

        if (roleId != null) {
            validator.validateId(roleId, "roleId");
        }

        if (permissionId != null) {
            validator.validateId(permissionId, "permissionId");
        }

        return repository.findRolePermissionHistory(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                roleId,
                permissionId
        );
    }

    @Transactional
    public PolicyRuleResponse createPolicyRule(CreatePolicyRuleRequest request) {
        validateContext();
        validator.validatePolicyRule(request);

        PolicyRuleResponse response = repository.createPolicyRule(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                request
        );

        internalAudit(
                "GOVERNANCE",
                "POLICY_RULE",
                response.ruleId(),
                "POLICY_RULE_CREATED",
                null,
                response.ruleStatus(),
                "Policy rule created."
        );

        return response;
    }

    public List<PolicyRuleResponse> getPolicyRules(String moduleName, String status) {
        validateContext();

        return repository.findPolicyRules(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                moduleName,
                status
        );
    }

    public PolicyRuleResponse getPolicyRule(Long ruleId) {
        validateContext();
        validator.validateId(ruleId, "ruleId");

        return repository.findPolicyRuleById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                ruleId
        );
    }

    @Transactional
    public PolicyRuleResponse updatePolicyRuleStatus(Long ruleId,
                                                     UpdatePolicyRuleStatusRequest request) {
        validateContext();
        validator.validateId(ruleId, "ruleId");
        validator.validatePolicyStatus(request);

        PolicyRuleResponse before = repository.findPolicyRuleById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                ruleId
        );

        PolicyRuleResponse response = repository.updatePolicyRuleStatus(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                ruleId,
                request.status()
        );

        internalAudit(
                "GOVERNANCE",
                "POLICY_RULE",
                ruleId,
                "POLICY_RULE_STATUS_UPDATED",
                before.ruleStatus(),
                response.ruleStatus(),
                request.reason()
        );

        return response;
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }

    private void internalAudit(String moduleName,
                               String entityType,
                               Long entityId,
                               String action,
                               String oldValue,
                               String newValue,
                               String description) {

        CreateAuditEventRequest request = new CreateAuditEventRequest(
                moduleName,
                entityType,
                entityId,
                action,
                "Success",
                oldValue,
                newValue,
                description,
                null,
                null,
                null
        );

        repository.createAuditEvent(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId(),
                request,
                "AUD-" + System.currentTimeMillis()
        );
    }
}