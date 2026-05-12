package com.plasmit.hospital.core.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HospitalCoreRepository {

    private static final Logger log = LoggerFactory.getLogger(HospitalCoreRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public HospitalCoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<HospitalProfile> findHospitalProfile(Long tenantId, Long hospitalId) {

        log.debug("Fetching hospital profile. tenantId={} hospitalId={}", tenantId, hospitalId);

        String sql = """
                SELECT
                    h.id,
                    h.tenant_id,
                    h.hospital_code,
                    h.hospital_name,
                    h.contact_person,
                    h.email,
                    h.phone,
                    h.address,
                    h.city,
                    h.state,
                    h.country,
                    h.pincode,
                    h.status
                FROM hospitals h
                WHERE h.tenant_id = ?
                  AND h.id = ?
                  AND h.status = 'ACTIVE'
                  AND h.is_deleted = 0
                LIMIT 1
                """;

        List<HospitalProfile> result = jdbcTemplate.query(sql, this::mapHospitalProfile, tenantId, hospitalId);

        return result.stream().findFirst();
    }

    public List<BranchOption> findBranches(Long tenantId, Long hospitalId) {

        log.debug("Fetching branches. tenantId={} hospitalId={}", tenantId, hospitalId);

        String sql = """
                SELECT
                    id,
                    branch_code,
                    branch_name,
                    city,
                    state,
                    status
                FROM hospital_branches
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                ORDER BY branch_name ASC
                """;

        return jdbcTemplate.query(sql, this::mapBranchOption, tenantId, hospitalId);
    }

    public List<DepartmentOption> findDepartments(Long tenantId, Long hospitalId, Long branchId) {

        log.debug("Fetching departments. tenantId={} hospitalId={} branchId={}",
                tenantId, hospitalId, branchId);

        String sql = """
                SELECT
                    id,
                    branch_id,
                    department_code,
                    department_name,
                    status
                FROM departments
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ? OR branch_id IS NULL)
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                ORDER BY department_name ASC
                """;

        return jdbcTemplate.query(sql, this::mapDepartmentOption, tenantId, hospitalId, branchId, branchId);
    }

    public List<ReferenceOption> findReferenceValues(Long tenantId, Long hospitalId, String referenceType) {

        log.debug("Fetching reference values. tenantId={} hospitalId={} referenceType={}",
                tenantId, hospitalId, referenceType);

        String sql = """
                SELECT
                    reference_code,
                    reference_label,
                    sort_order
                FROM reference_values
                WHERE reference_type = ?
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                  AND (
                        (tenant_id = ? AND hospital_id = ?)
                        OR (tenant_id IS NULL AND hospital_id IS NULL)
                  )
                ORDER BY sort_order ASC, reference_label ASC
                """;

        return jdbcTemplate.query(sql, this::mapReferenceOption, referenceType, tenantId, hospitalId);
    }

    public List<SettingRecord> findSettings(Long tenantId, Long hospitalId) {

        log.debug("Fetching hospital settings. tenantId={} hospitalId={}", tenantId, hospitalId);

        String sql = """
                SELECT
                    setting_key,
                    setting_value,
                    setting_type,
                    setting_group
                FROM hospital_settings
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND is_deleted = 0
                ORDER BY setting_group ASC, setting_key ASC
                """;

        return jdbcTemplate.query(sql, this::mapSettingRecord, tenantId, hospitalId);
    }

    private HospitalProfile mapHospitalProfile(ResultSet rs, int rowNum) throws SQLException {
        return new HospitalProfile(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getString("hospital_code"),
                rs.getString("hospital_name"),
                rs.getString("contact_person"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("country"),
                rs.getString("pincode"),
                rs.getString("status")
        );
    }

    private BranchOption mapBranchOption(ResultSet rs, int rowNum) throws SQLException {
        return new BranchOption(
                rs.getLong("id"),
                rs.getString("branch_code"),
                rs.getString("branch_name"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("status")
        );
    }

    private DepartmentOption mapDepartmentOption(ResultSet rs, int rowNum) throws SQLException {
        Long branchId = rs.getLong("branch_id");
        if (rs.wasNull()) {
            branchId = null;
        }

        return new DepartmentOption(
                rs.getLong("id"),
                branchId,
                rs.getString("department_code"),
                rs.getString("department_name"),
                rs.getString("status")
        );
    }

    private ReferenceOption mapReferenceOption(ResultSet rs, int rowNum) throws SQLException {
        return new ReferenceOption(
                rs.getString("reference_code"),
                rs.getString("reference_label"),
                rs.getInt("sort_order")
        );
    }

    private SettingRecord mapSettingRecord(ResultSet rs, int rowNum) throws SQLException {
        return new SettingRecord(
                rs.getString("setting_key"),
                rs.getString("setting_value"),
                rs.getString("setting_type"),
                rs.getString("setting_group")
        );
    }

    public record HospitalProfile(
            Long id,
            Long tenantId,
            String code,
            String name,
            String contactPerson,
            String email,
            String phone,
            String address,
            String city,
            String state,
            String country,
            String pincode,
            String status
    ) {
    }

    public record BranchOption(
            Long id,
            String code,
            String name,
            String city,
            String state,
            String status
    ) {
    }

    public record DepartmentOption(
            Long id,
            Long branchId,
            String code,
            String name,
            String status
    ) {
    }

    public record ReferenceOption(
            String code,
            String label,
            Integer sortOrder
    ) {
    }

    public record SettingRecord(
            String key,
            String value,
            String type,
            String group
    ) {
    }
}