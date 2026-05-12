package com.plasmit.auth.hospital.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserRecord> findActiveUserByEmail(String email) {

        log.debug("Finding active hospital user by email.");

        String sql = """
                SELECT
                    u.id,
                    u.tenant_id,
                    u.hospital_id,
                    u.branch_id,
                    u.department_id,
                    u.full_name,
                    u.email,
                    u.phone,
                    u.password_hash,
                    u.user_type,
                    u.status,
                    u.last_login_at,
                    h.hospital_name,
                    b.branch_name,
                    d.department_name,
                    r.role_code,
                    r.role_name
                FROM users u
                INNER JOIN hospitals h
                    ON h.id = u.hospital_id
                    AND h.tenant_id = u.tenant_id
                    AND h.status = 'ACTIVE'
                    AND h.is_deleted = 0
                LEFT JOIN hospital_branches b
                    ON b.id = u.branch_id
                    AND b.tenant_id = u.tenant_id
                    AND b.hospital_id = u.hospital_id
                    AND b.is_deleted = 0
                LEFT JOIN departments d
                    ON d.id = u.department_id
                    AND d.tenant_id = u.tenant_id
                    AND d.hospital_id = u.hospital_id
                    AND d.is_deleted = 0
                INNER JOIN user_roles ur
                    ON ur.user_id = u.id
                    AND ur.tenant_id = u.tenant_id
                    AND ur.hospital_id = u.hospital_id
                INNER JOIN roles r
                    ON r.id = ur.role_id
                    AND r.tenant_id = u.tenant_id
                    AND r.hospital_id = u.hospital_id
                    AND r.status = 'ACTIVE'
                    AND r.is_deleted = 0
                WHERE LOWER(u.email) = LOWER(?)
                  AND u.status = 'ACTIVE'
                  AND u.is_deleted = 0
                LIMIT 1
                """;

        List<UserRecord> users = jdbcTemplate.query(sql, this::mapUser, email);

        if (users.isEmpty()) {
            return Optional.empty();
        }

        UserRecord user = users.get(0);
        List<String> permissions = loadPermissions(user.tenantId(), user.hospitalId(), user.roleCode());

        return Optional.of(user.withPermissions(permissions));
    }

    public Optional<UserRecord> findActiveUserByIdAndTenant(Long userId, Long tenantId, Long hospitalId) {

        log.debug("Finding active hospital user with tenant filter. userId={} tenantId={} hospitalId={}",
                userId, tenantId, hospitalId);

        String sql = """
                SELECT
                    u.id,
                    u.tenant_id,
                    u.hospital_id,
                    u.branch_id,
                    u.department_id,
                    u.full_name,
                    u.email,
                    u.phone,
                    u.password_hash,
                    u.user_type,
                    u.status,
                    u.last_login_at,
                    h.hospital_name,
                    b.branch_name,
                    d.department_name,
                    r.role_code,
                    r.role_name
                FROM users u
                INNER JOIN hospitals h
                    ON h.id = u.hospital_id
                    AND h.tenant_id = u.tenant_id
                    AND h.status = 'ACTIVE'
                    AND h.is_deleted = 0
                LEFT JOIN hospital_branches b
                    ON b.id = u.branch_id
                    AND b.tenant_id = u.tenant_id
                    AND b.hospital_id = u.hospital_id
                    AND b.is_deleted = 0
                LEFT JOIN departments d
                    ON d.id = u.department_id
                    AND d.tenant_id = u.tenant_id
                    AND d.hospital_id = u.hospital_id
                    AND d.is_deleted = 0
                INNER JOIN user_roles ur
                    ON ur.user_id = u.id
                    AND ur.tenant_id = u.tenant_id
                    AND ur.hospital_id = u.hospital_id
                INNER JOIN roles r
                    ON r.id = ur.role_id
                    AND r.tenant_id = u.tenant_id
                    AND r.hospital_id = u.hospital_id
                    AND r.status = 'ACTIVE'
                    AND r.is_deleted = 0
                WHERE u.id = ?
                  AND u.tenant_id = ?
                  AND u.hospital_id = ?
                  AND u.status = 'ACTIVE'
                  AND u.is_deleted = 0
                LIMIT 1
                """;

        List<UserRecord> users = jdbcTemplate.query(sql, this::mapUser, userId, tenantId, hospitalId);

        if (users.isEmpty()) {
            return Optional.empty();
        }

        UserRecord user = users.get(0);
        List<String> permissions = loadPermissions(user.tenantId(), user.hospitalId(), user.roleCode());

        return Optional.of(user.withPermissions(permissions));
    }

    public void updateLastLogin(Long userId) {
        jdbcTemplate.update("""
                UPDATE users
                SET last_login_at = NOW(),
                    failed_login_attempts = 0,
                    updated_at = NOW()
                WHERE id = ?
                """, userId);
    }

    public void saveLoginActivity(
            Long tenantId,
            Long hospitalId,
            Long userId,
            String email,
            String status,
            String failureReason,
            String ipAddress,
            String userAgent
    ) {
        jdbcTemplate.update("""
                INSERT INTO login_activity (
                    tenant_id,
                    hospital_id,
                    user_id,
                    email,
                    login_status,
                    failure_reason,
                    ip_address,
                    user_agent,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """, tenantId, hospitalId, userId, email, status, failureReason, ipAddress, userAgent);
    }

    public void saveAuditLog(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long actorUserId,
            String action,
            String message,
            String requestId,
            String ipAddress,
            String userAgent
    ) {
        jdbcTemplate.update("""
                INSERT INTO audit_logs (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    actor_id,
                    event,
                    target_type,
                    target_id,
                    detail,
                    request_id,
                    ip_address,
                    user_agent,
                    status,
                    category,
                    severity,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, 'USER', ?, ?, ?, ?, ?, 'SUCCESS', 'AUTH', 'LOW', NOW())
                """,
                tenantId,
                hospitalId,
                branchId,
                actorUserId,
                action,
                actorUserId == null ? null : actorUserId,
                message,
                requestId,
                ipAddress,
                userAgent
        );
    }
    
    public void resetPasswordByEmail(String email, String passwordHash, Long updatedBy) {
        jdbcTemplate.update("""
                UPDATE users
                SET password_hash = ?,
                    failed_login_attempts = 0,
                    locked_until = NULL,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE LOWER(email) = LOWER(?)
                  AND is_deleted = 0
                """,
                passwordHash,
                updatedBy,
                email
        );
    }

    private List<String> loadPermissions(Long tenantId, Long hospitalId, String roleCode) {

        String sql = """
                SELECT p.permission_key
                FROM permissions p
                INNER JOIN role_permissions rp
                    ON rp.permission_id = p.id
                INNER JOIN roles r
                    ON r.id = rp.role_id
                WHERE rp.tenant_id = ?
                  AND rp.hospital_id = ?
                  AND r.role_code = ?
                  AND r.status = 'ACTIVE'
                  AND r.is_deleted = 0
                  AND p.status = 'ACTIVE'
                ORDER BY p.permission_key
                """;

        return jdbcTemplate.queryForList(sql, String.class, tenantId, hospitalId, roleCode);
    }

    private UserRecord mapUser(ResultSet rs, int rowNum) throws SQLException {

        return new UserRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                getNullableLong(rs, "department_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("password_hash"),
                rs.getString("user_type"),
                rs.getString("status"),
                rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toLocalDateTime().toString(),
                rs.getString("hospital_name"),
                rs.getString("branch_name"),
                rs.getString("department_name"),
                rs.getString("role_code"),
                rs.getString("role_name"),
                new ArrayList<>()
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public record UserRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            String fullName,
            String email,
            String phone,
            String passwordHash,
            String userType,
            String status,
            String lastLoginAt,
            String hospitalName,
            String branchName,
            String departmentName,
            String roleCode,
            String roleName,
            List<String> permissions
    ) {
        public UserRecord withPermissions(List<String> permissions) {
            return new UserRecord(
                    id,
                    tenantId,
                    hospitalId,
                    branchId,
                    departmentId,
                    fullName,
                    email,
                    phone,
                    passwordHash,
                    userType,
                    status,
                    lastLoginAt,
                    hospitalName,
                    branchName,
                    departmentName,
                    roleCode,
                    roleName,
                    permissions
            );
        }
    }
}