package com.plasmit.diagnostic.governance.governance.repository;

import com.plasmit.diagnostic.governance.common.exception.ApiException;
import com.plasmit.diagnostic.governance.governance.dto.request.*;
import com.plasmit.diagnostic.governance.governance.dto.response.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class DiagnosticGovernanceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DiagnosticGovernanceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long countAuditEvents(Long tenantId,
                                 Long hospitalId,
                                 String branchId,
                                 LocalDate fromDate,
                                 LocalDate toDate,
                                 String moduleName,
                                 String entityType,
                                 String action,
                                 String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM diagnostic_governance_audit_events
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                """);

        MapSqlParameterSource params = auditParams(
                tenantId, hospitalId, branchId, fromDate, toDate,
                moduleName, entityType, action, search
        );

        appendAuditFilters(sql, moduleName, entityType, action, search);

        Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0L : total;
    }

    public List<AuditEventResponse> findAuditEvents(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String moduleName,
                                                    String entityType,
                                                    String action,
                                                    String search,
                                                    Integer page,
                                                    Integer limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_governance_audit_events
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                """);

        MapSqlParameterSource params = auditParams(
                tenantId, hospitalId, branchId, fromDate, toDate,
                moduleName, entityType, action, search
        );

        appendAuditFilters(sql, moduleName, entityType, action, search);

        sql.append("""
                ORDER BY created_at DESC
                LIMIT :limit OFFSET :offset
                """);

        params.addValue("limit", limit);
        params.addValue("offset", (page - 1) * limit);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapAudit(rs));
    }

    public AuditEventResponse createAuditEvent(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long userId,
                                               String role,
                                               String requestId,
                                               CreateAuditEventRequest request,
                                               String eventCode) {

        String sql = """
                INSERT INTO diagnostic_governance_audit_events (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    event_code,
                    module_name,
                    entity_type,
                    entity_id,
                    action,
                    action_status,
                    old_value,
                    new_value,
                    description,
                    actor_user_id,
                    actor_name,
                    actor_role,
                    ip_address,
                    user_agent,
                    request_id
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :eventCode,
                    :moduleName,
                    :entityType,
                    :entityId,
                    :action,
                    :actionStatus,
                    :oldValue,
                    :newValue,
                    :description,
                    :actorUserId,
                    :actorName,
                    :actorRole,
                    :ipAddress,
                    :userAgent,
                    :requestId
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("eventCode", eventCode)
                .addValue("moduleName", request.moduleName())
                .addValue("entityType", request.entityType())
                .addValue("entityId", request.entityId())
                .addValue("action", request.action())
                .addValue("actionStatus", request.actionStatus() == null || request.actionStatus().isBlank() ? "Success" : request.actionStatus())
                .addValue("oldValue", request.oldValue())
                .addValue("newValue", request.newValue())
                .addValue("description", request.description())
                .addValue("actorUserId", userId)
                .addValue("actorName", request.actorName())
                .addValue("actorRole", role)
                .addValue("ipAddress", request.ipAddress())
                .addValue("userAgent", request.userAgent())
                .addValue("requestId", requestId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return findAuditById(tenantId, hospitalId, branchId, Objects.requireNonNull(keyHolder.getKey()).longValue());
    }

    public AuditEventResponse findAuditById(Long tenantId, Long hospitalId, String branchId, Long auditId) {
        String sql = """
                SELECT *
                FROM diagnostic_governance_audit_events
                WHERE id = :auditId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                """;

        List<AuditEventResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("auditId", auditId),
                (rs, rowNum) -> mapAudit(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Audit event not found.");
        }

        return rows.get(0);
    }

    public RoleResponse createRole(Long tenantId,
                                   Long hospitalId,
                                   String branchId,
                                   Long userId,
                                   CreateRoleRequest request) {

        String sql = """
                INSERT INTO diagnostic_governance_roles (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    role_code,
                    role_name,
                    role_description,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :roleCode,
                    :roleName,
                    :roleDescription,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("roleCode", request.roleCode())
                .addValue("roleName", request.roleName())
                .addValue("roleDescription", request.roleDescription())
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return findRoleById(tenantId, hospitalId, branchId, Objects.requireNonNull(keyHolder.getKey()).longValue());
    }

    public List<RoleResponse> findRoles(Long tenantId,
                                        Long hospitalId,
                                        String branchId,
                                        String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_governance_roles
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY role_name ASC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapRole(rs));
    }

    public RoleResponse findRoleById(Long tenantId,
                                     Long hospitalId,
                                     String branchId,
                                     Long roleId) {

        String sql = """
                SELECT *
                FROM diagnostic_governance_roles
                WHERE id = :roleId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<RoleResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("roleId", roleId),
                (rs, rowNum) -> mapRole(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Governance role not found.");
        }

        return rows.get(0);
    }

    public PermissionResponse createPermission(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long userId,
                                               CreatePermissionRequest request) {

        String sql = """
                INSERT INTO diagnostic_governance_permissions (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    permission_code,
                    permission_name,
                    module_name,
                    permission_description,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :permissionCode,
                    :permissionName,
                    :moduleName,
                    :permissionDescription,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("permissionCode", request.permissionCode())
                .addValue("permissionName", request.permissionName())
                .addValue("moduleName", request.moduleName())
                .addValue("permissionDescription", request.permissionDescription())
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return findPermissionById(tenantId, hospitalId, branchId, Objects.requireNonNull(keyHolder.getKey()).longValue());
    }

    public List<PermissionResponse> findPermissions(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    String moduleName,
                                                    String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_governance_permissions
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (moduleName != null && !moduleName.isBlank()) {
            sql.append(" AND module_name = :moduleName ");
            params.addValue("moduleName", moduleName);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY module_name ASC, permission_name ASC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapPermission(rs));
    }

    public PermissionResponse findPermissionById(Long tenantId,
                                                 Long hospitalId,
                                                 String branchId,
                                                 Long permissionId) {

        String sql = """
                SELECT *
                FROM diagnostic_governance_permissions
                WHERE id = :permissionId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<PermissionResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("permissionId", permissionId),
                (rs, rowNum) -> mapPermission(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Governance permission not found.");
        }

        return rows.get(0);
    }

    public RolePermissionResponse grantPermission(Long tenantId,
                                                  Long hospitalId,
                                                  String branchId,
                                                  Long userId,
                                                  Long roleId,
                                                  Long permissionId,
                                                  String reason,
                                                  String requestId) {

        findRoleById(tenantId, hospitalId, branchId, roleId);
        findPermissionById(tenantId, hospitalId, branchId, permissionId);

        RolePermissionResponse existing = findRolePermissionNullable(
                tenantId,
                hospitalId,
                branchId,
                roleId,
                permissionId
        );

        if (existing == null) {
            String insertSql = """
                    INSERT INTO diagnostic_governance_role_permissions (
                        tenant_id,
                        hospital_id,
                        branch_id,
                        role_id,
                        permission_id,
                        access_status,
                        granted_by
                    ) VALUES (
                        :tenantId,
                        :hospitalId,
                        :branchId,
                        :roleId,
                        :permissionId,
                        'Granted',
                        :grantedBy
                    )
                    """;

            jdbc.update(insertSql, baseParams(tenantId, hospitalId, branchId)
                    .addValue("roleId", roleId)
                    .addValue("permissionId", permissionId)
                    .addValue("grantedBy", userId));

            insertRolePermissionHistory(
                    tenantId,
                    hospitalId,
                    branchId,
                    roleId,
                    permissionId,
                    null,
                    "Granted",
                    reason,
                    userId,
                    requestId
            );

            return findRolePermission(tenantId, hospitalId, branchId, roleId, permissionId);
        }

        if ("Granted".equals(existing.accessStatus())) {
            throw ApiException.workflow("Permission is already granted to this role.");
        }

        String updateSql = """
                UPDATE diagnostic_governance_role_permissions
                SET access_status = 'Granted',
                    granted_by = :grantedBy,
                    granted_at = NOW(),
                    revoked_by = NULL,
                    revoked_at = NULL,
                    revoke_reason = NULL,
                    is_deleted = 0
                WHERE role_id = :roleId
                  AND permission_id = :permissionId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                """;

        jdbc.update(updateSql, baseParams(tenantId, hospitalId, branchId)
                .addValue("roleId", roleId)
                .addValue("permissionId", permissionId)
                .addValue("grantedBy", userId));

        insertRolePermissionHistory(
                tenantId,
                hospitalId,
                branchId,
                roleId,
                permissionId,
                existing.accessStatus(),
                "Granted",
                reason,
                userId,
                requestId
        );

        return findRolePermission(tenantId, hospitalId, branchId, roleId, permissionId);
    }

    public RolePermissionResponse revokePermission(Long tenantId,
                                                   Long hospitalId,
                                                   String branchId,
                                                   Long userId,
                                                   Long rolePermissionId,
                                                   String reason,
                                                   String requestId) {

        RolePermissionResponse existing = findRolePermissionById(tenantId, hospitalId, branchId, rolePermissionId);

        if ("Revoked".equals(existing.accessStatus())) {
            throw ApiException.workflow("Permission is already revoked.");
        }

        String sql = """
                UPDATE diagnostic_governance_role_permissions
                SET access_status = 'Revoked',
                    revoked_by = :revokedBy,
                    revoked_at = NOW(),
                    revoke_reason = :revokeReason
                WHERE id = :rolePermissionId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("rolePermissionId", rolePermissionId)
                .addValue("revokedBy", userId)
                .addValue("revokeReason", reason));

        insertRolePermissionHistory(
                tenantId,
                hospitalId,
                branchId,
                existing.roleId(),
                existing.permissionId(),
                existing.accessStatus(),
                "Revoked",
                reason,
                userId,
                requestId
        );

        return findRolePermissionById(tenantId, hospitalId, branchId, rolePermissionId);
    }

    public List<RolePermissionResponse> findRolePermissions(Long tenantId,
                                                           Long hospitalId,
                                                           String branchId,
                                                           Long roleId,
                                                           String accessStatus) {

        StringBuilder sql = new StringBuilder("""
                SELECT rp.*,
                       r.role_code,
                       r.role_name,
                       p.permission_code,
                       p.permission_name,
                       p.module_name
                FROM diagnostic_governance_role_permissions rp
                JOIN diagnostic_governance_roles r ON r.id = rp.role_id
                JOIN diagnostic_governance_permissions p ON p.id = rp.permission_id
                WHERE rp.tenant_id = :tenantId
                  AND rp.hospital_id = :hospitalId
                  AND rp.branch_id = :branchId
                  AND rp.is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (roleId != null) {
            sql.append(" AND rp.role_id = :roleId ");
            params.addValue("roleId", roleId);
        }

        if (accessStatus != null && !accessStatus.isBlank()) {
            sql.append(" AND rp.access_status = :accessStatus ");
            params.addValue("accessStatus", accessStatus);
        }

        sql.append(" ORDER BY r.role_name ASC, p.permission_name ASC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapRolePermission(rs));
    }

    public RolePermissionResponse findRolePermissionById(Long tenantId,
                                                        Long hospitalId,
                                                        String branchId,
                                                        Long rolePermissionId) {

        String sql = """
                SELECT rp.*,
                       r.role_code,
                       r.role_name,
                       p.permission_code,
                       p.permission_name,
                       p.module_name
                FROM diagnostic_governance_role_permissions rp
                JOIN diagnostic_governance_roles r ON r.id = rp.role_id
                JOIN diagnostic_governance_permissions p ON p.id = rp.permission_id
                WHERE rp.id = :rolePermissionId
                  AND rp.tenant_id = :tenantId
                  AND rp.hospital_id = :hospitalId
                  AND rp.branch_id = :branchId
                  AND rp.is_deleted = 0
                """;

        List<RolePermissionResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("rolePermissionId", rolePermissionId),
                (rs, rowNum) -> mapRolePermission(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Role permission mapping not found.");
        }

        return rows.get(0);
    }

    public List<RolePermissionHistoryResponse> findRolePermissionHistory(Long tenantId,
                                                                         Long hospitalId,
                                                                         String branchId,
                                                                         Long roleId,
                                                                         Long permissionId) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_governance_role_permission_history
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (roleId != null) {
            sql.append(" AND role_id = :roleId ");
            params.addValue("roleId", roleId);
        }

        if (permissionId != null) {
            sql.append(" AND permission_id = :permissionId ");
            params.addValue("permissionId", permissionId);
        }

        sql.append(" ORDER BY changed_at DESC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> new RolePermissionHistoryResponse(
                rs.getLong("id"),
                rs.getLong("role_id"),
                rs.getLong("permission_id"),
                rs.getString("from_status"),
                rs.getString("to_status"),
                rs.getString("change_reason"),
                rs.getObject("changed_by") == null ? null : rs.getLong("changed_by"),
                rs.getString("request_id"),
                rs.getTimestamp("changed_at") == null ? null : rs.getTimestamp("changed_at").toLocalDateTime()
        ));
    }

    public PolicyRuleResponse createPolicyRule(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long userId,
                                               CreatePolicyRuleRequest request) {

        String sql = """
                INSERT INTO diagnostic_governance_policy_rules (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    rule_code,
                    rule_name,
                    module_name,
                    rule_type,
                    rule_config,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :ruleCode,
                    :ruleName,
                    :moduleName,
                    :ruleType,
                    :ruleConfig,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("ruleCode", request.ruleCode())
                .addValue("ruleName", request.ruleName())
                .addValue("moduleName", request.moduleName())
                .addValue("ruleType", request.ruleType())
                .addValue("ruleConfig", request.ruleConfig())
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return findPolicyRuleById(tenantId, hospitalId, branchId, Objects.requireNonNull(keyHolder.getKey()).longValue());
    }

    public List<PolicyRuleResponse> findPolicyRules(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    String moduleName,
                                                    String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_governance_policy_rules
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (moduleName != null && !moduleName.isBlank()) {
            sql.append(" AND module_name = :moduleName ");
            params.addValue("moduleName", moduleName);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND rule_status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY module_name ASC, rule_name ASC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapPolicyRule(rs));
    }

    public PolicyRuleResponse findPolicyRuleById(Long tenantId,
                                                 Long hospitalId,
                                                 String branchId,
                                                 Long ruleId) {

        String sql = """
                SELECT *
                FROM diagnostic_governance_policy_rules
                WHERE id = :ruleId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<PolicyRuleResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("ruleId", ruleId),
                (rs, rowNum) -> mapPolicyRule(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Policy rule not found.");
        }

        return rows.get(0);
    }

    public PolicyRuleResponse updatePolicyRuleStatus(Long tenantId,
                                                     Long hospitalId,
                                                     String branchId,
                                                     Long userId,
                                                     Long ruleId,
                                                     String status) {

        findPolicyRuleById(tenantId, hospitalId, branchId, ruleId);

        String sql = """
                UPDATE diagnostic_governance_policy_rules
                SET rule_status = :status,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :ruleId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("ruleId", ruleId)
                .addValue("status", status)
                .addValue("updatedBy", userId));

        return findPolicyRuleById(tenantId, hospitalId, branchId, ruleId);
    }

    private RolePermissionResponse findRolePermission(Long tenantId,
                                                      Long hospitalId,
                                                      String branchId,
                                                      Long roleId,
                                                      Long permissionId) {

        String sql = """
                SELECT rp.*,
                       r.role_code,
                       r.role_name,
                       p.permission_code,
                       p.permission_name,
                       p.module_name
                FROM diagnostic_governance_role_permissions rp
                JOIN diagnostic_governance_roles r ON r.id = rp.role_id
                JOIN diagnostic_governance_permissions p ON p.id = rp.permission_id
                WHERE rp.role_id = :roleId
                  AND rp.permission_id = :permissionId
                  AND rp.tenant_id = :tenantId
                  AND rp.hospital_id = :hospitalId
                  AND rp.branch_id = :branchId
                  AND rp.is_deleted = 0
                """;

        List<RolePermissionResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId)
                        .addValue("roleId", roleId)
                        .addValue("permissionId", permissionId),
                (rs, rowNum) -> mapRolePermission(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Role permission mapping not found.");
        }

        return rows.get(0);
    }

    private RolePermissionResponse findRolePermissionNullable(Long tenantId,
                                                              Long hospitalId,
                                                              String branchId,
                                                              Long roleId,
                                                              Long permissionId) {
        String sql = """
                SELECT rp.*,
                       r.role_code,
                       r.role_name,
                       p.permission_code,
                       p.permission_name,
                       p.module_name
                FROM diagnostic_governance_role_permissions rp
                JOIN diagnostic_governance_roles r ON r.id = rp.role_id
                JOIN diagnostic_governance_permissions p ON p.id = rp.permission_id
                WHERE rp.role_id = :roleId
                  AND rp.permission_id = :permissionId
                  AND rp.tenant_id = :tenantId
                  AND rp.hospital_id = :hospitalId
                  AND rp.branch_id = :branchId
                """;

        List<RolePermissionResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId)
                        .addValue("roleId", roleId)
                        .addValue("permissionId", permissionId),
                (rs, rowNum) -> mapRolePermission(rs)
        );

        return rows.isEmpty() ? null : rows.get(0);
    }

    private void insertRolePermissionHistory(Long tenantId,
                                             Long hospitalId,
                                             String branchId,
                                             Long roleId,
                                             Long permissionId,
                                             String fromStatus,
                                             String toStatus,
                                             String reason,
                                             Long userId,
                                             String requestId) {

        String sql = """
                INSERT INTO diagnostic_governance_role_permission_history (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    role_id,
                    permission_id,
                    from_status,
                    to_status,
                    change_reason,
                    changed_by,
                    request_id
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :roleId,
                    :permissionId,
                    :fromStatus,
                    :toStatus,
                    :changeReason,
                    :changedBy,
                    :requestId
                )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("roleId", roleId)
                .addValue("permissionId", permissionId)
                .addValue("fromStatus", fromStatus)
                .addValue("toStatus", toStatus)
                .addValue("changeReason", reason)
                .addValue("changedBy", userId)
                .addValue("requestId", requestId));
    }

    private AuditEventResponse mapAudit(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AuditEventResponse(
                rs.getLong("id"),
                rs.getString("event_code"),
                rs.getString("module_name"),
                rs.getString("entity_type"),
                rs.getObject("entity_id") == null ? null : rs.getLong("entity_id"),
                rs.getString("action"),
                rs.getString("action_status"),
                rs.getString("old_value"),
                rs.getString("new_value"),
                rs.getString("description"),
                rs.getObject("actor_user_id") == null ? null : rs.getLong("actor_user_id"),
                rs.getString("actor_name"),
                rs.getString("actor_role"),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getString("request_id"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private RoleResponse mapRole(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RoleResponse(
                rs.getLong("id"),
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getString("role_description"),
                rs.getString("status"),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private PermissionResponse mapPermission(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PermissionResponse(
                rs.getLong("id"),
                rs.getString("permission_code"),
                rs.getString("permission_name"),
                rs.getString("module_name"),
                rs.getString("permission_description"),
                rs.getString("status"),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private RolePermissionResponse mapRolePermission(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RolePermissionResponse(
                rs.getLong("id"),
                rs.getLong("role_id"),
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getLong("permission_id"),
                rs.getString("permission_code"),
                rs.getString("permission_name"),
                rs.getString("module_name"),
                rs.getString("access_status"),
                rs.getObject("granted_by") == null ? null : rs.getLong("granted_by"),
                rs.getTimestamp("granted_at") == null ? null : rs.getTimestamp("granted_at").toLocalDateTime(),
                rs.getObject("revoked_by") == null ? null : rs.getLong("revoked_by"),
                rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toLocalDateTime(),
                rs.getString("revoke_reason")
        );
    }

    private PolicyRuleResponse mapPolicyRule(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PolicyRuleResponse(
                rs.getLong("id"),
                rs.getString("rule_code"),
                rs.getString("rule_name"),
                rs.getString("module_name"),
                rs.getString("rule_type"),
                rs.getString("rule_config"),
                rs.getString("rule_status"),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private void appendAuditFilters(StringBuilder sql,
                                    String moduleName,
                                    String entityType,
                                    String action,
                                    String search) {

        if (moduleName != null && !moduleName.isBlank()) {
            sql.append(" AND module_name = :moduleName ");
        }

        if (entityType != null && !entityType.isBlank()) {
            sql.append(" AND entity_type = :entityType ");
        }

        if (action != null && !action.isBlank()) {
            sql.append(" AND action = :action ");
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        event_code LIKE :search
                        OR description LIKE :search
                        OR actor_name LIKE :search
                        OR request_id LIKE :search
                    )
                    """);
        }
    }

    private MapSqlParameterSource auditParams(Long tenantId,
                                              Long hospitalId,
                                              String branchId,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              String moduleName,
                                              String entityType,
                                              String action,
                                              String search) {

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);

        if (moduleName != null && !moduleName.isBlank()) {
            params.addValue("moduleName", moduleName);
        }

        if (entityType != null && !entityType.isBlank()) {
            params.addValue("entityType", entityType);
        }

        if (action != null && !action.isBlank()) {
            params.addValue("action", action);
        }

        if (search != null && !search.isBlank()) {
            params.addValue("search", "%" + search.trim() + "%");
        }

        return params;
    }

    private MapSqlParameterSource baseParams(Long tenantId, Long hospitalId, String branchId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("branchId", branchId);
    }
}