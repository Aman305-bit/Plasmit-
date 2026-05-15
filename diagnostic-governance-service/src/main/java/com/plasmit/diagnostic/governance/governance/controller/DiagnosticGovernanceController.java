package com.plasmit.diagnostic.governance.governance.controller;

import com.plasmit.diagnostic.governance.common.response.ApiResponse;
import com.plasmit.diagnostic.governance.common.response.PageResponse;
import com.plasmit.diagnostic.governance.governance.dto.request.*;
import com.plasmit.diagnostic.governance.governance.dto.response.*;
import com.plasmit.diagnostic.governance.governance.service.DiagnosticGovernanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital/diagnostic-governance")
@CrossOrigin("*")
public class DiagnosticGovernanceController {

    private final DiagnosticGovernanceService service;

    public DiagnosticGovernanceController(DiagnosticGovernanceService service) {
        this.service = service;
    }

    @GetMapping("/audit-events")
    public ApiResponse<PageResponse<AuditEventResponse>> getAuditEvents(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "moduleName", required = false)
            String moduleName,

            @RequestParam(value = "entityType", required = false)
            String entityType,

            @RequestParam(value = "action", required = false)
            String action,

            @RequestParam(value = "search", required = false)
            String search,

            @RequestParam(value = "page", defaultValue = "1")
            Integer page,

            @RequestParam(value = "limit", defaultValue = "25")
            Integer limit
    ) {
        return ApiResponse.success(
                "Governance audit events fetched successfully.",
                service.getAuditEvents(fromDate, toDate, moduleName, entityType, action, search, page, limit)
        );
    }

    @PostMapping("/audit-events")
    public ApiResponse<AuditEventResponse> createAuditEvent(
            @Valid @RequestBody CreateAuditEventRequest request
    ) {
        return ApiResponse.success(
                "Governance audit event created successfully.",
                service.createAuditEvent(request)
        );
    }

    @PostMapping("/roles")
    public ApiResponse<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request
    ) {
        return ApiResponse.success(
                "Governance role created successfully.",
                service.createRole(request)
        );
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> getRoles(
            @RequestParam(value = "status", required = false)
            String status
    ) {
        return ApiResponse.success(
                "Governance roles fetched successfully.",
                service.getRoles(status)
        );
    }

    @GetMapping("/roles/{roleId}")
    public ApiResponse<RoleResponse> getRole(@PathVariable Long roleId) {
        return ApiResponse.success(
                "Governance role fetched successfully.",
                service.getRole(roleId)
        );
    }

    @PostMapping("/permissions")
    public ApiResponse<PermissionResponse> createPermission(
            @Valid @RequestBody CreatePermissionRequest request
    ) {
        return ApiResponse.success(
                "Governance permission created successfully.",
                service.createPermission(request)
        );
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionResponse>> getPermissions(
            @RequestParam(value = "moduleName", required = false)
            String moduleName,

            @RequestParam(value = "status", required = false)
            String status
    ) {
        return ApiResponse.success(
                "Governance permissions fetched successfully.",
                service.getPermissions(moduleName, status)
        );
    }

    @GetMapping("/permissions/{permissionId}")
    public ApiResponse<PermissionResponse> getPermission(@PathVariable Long permissionId) {
        return ApiResponse.success(
                "Governance permission fetched successfully.",
                service.getPermission(permissionId)
        );
    }

    @PostMapping("/roles/{roleId}/permissions")
    public ApiResponse<RolePermissionResponse> grantPermission(
            @PathVariable Long roleId,
            @Valid @RequestBody GrantPermissionRequest request
    ) {
        return ApiResponse.success(
                "Permission granted to role successfully.",
                service.grantPermission(roleId, request)
        );
    }

    @PostMapping("/role-permissions/{rolePermissionId}/revoke")
    public ApiResponse<RolePermissionResponse> revokePermission(
            @PathVariable Long rolePermissionId,
            @Valid @RequestBody RevokePermissionRequest request
    ) {
        return ApiResponse.success(
                "Role permission revoked successfully.",
                service.revokePermission(rolePermissionId, request)
        );
    }

    @GetMapping("/role-permissions")
    public ApiResponse<List<RolePermissionResponse>> getRolePermissions(
            @RequestParam(value = "roleId", required = false)
            Long roleId,

            @RequestParam(value = "accessStatus", required = false)
            String accessStatus
    ) {
        return ApiResponse.success(
                "Role permissions fetched successfully.",
                service.getRolePermissions(roleId, accessStatus)
        );
    }

    @GetMapping("/role-permissions/history")
    public ApiResponse<List<RolePermissionHistoryResponse>> getRolePermissionHistory(
            @RequestParam(value = "roleId", required = false)
            Long roleId,

            @RequestParam(value = "permissionId", required = false)
            Long permissionId
    ) {
        return ApiResponse.success(
                "Role permission history fetched successfully.",
                service.getRolePermissionHistory(roleId, permissionId)
        );
    }

    @PostMapping("/policy-rules")
    public ApiResponse<PolicyRuleResponse> createPolicyRule(
            @Valid @RequestBody CreatePolicyRuleRequest request
    ) {
        return ApiResponse.success(
                "Policy rule created successfully.",
                service.createPolicyRule(request)
        );
    }

    @GetMapping("/policy-rules")
    public ApiResponse<List<PolicyRuleResponse>> getPolicyRules(
            @RequestParam(value = "moduleName", required = false)
            String moduleName,

            @RequestParam(value = "status", required = false)
            String status
    ) {
        return ApiResponse.success(
                "Policy rules fetched successfully.",
                service.getPolicyRules(moduleName, status)
        );
    }

    @GetMapping("/policy-rules/{ruleId}")
    public ApiResponse<PolicyRuleResponse> getPolicyRule(@PathVariable Long ruleId) {
        return ApiResponse.success(
                "Policy rule fetched successfully.",
                service.getPolicyRule(ruleId)
        );
    }

    @PostMapping("/policy-rules/{ruleId}/status")
    public ApiResponse<PolicyRuleResponse> updatePolicyRuleStatus(
            @PathVariable Long ruleId,
            @Valid @RequestBody UpdatePolicyRuleStatusRequest request
    ) {
        return ApiResponse.success(
                "Policy rule status updated successfully.",
                service.updatePolicyRuleStatus(ruleId, request)
        );
    }
}