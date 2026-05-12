package com.plasmit.patient.hospital.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.plasmit.patient.hospital.service.PatientService.CreatePatientRequest;
import com.plasmit.patient.hospital.service.PatientService.UpdatePatientRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PatientRepository {

    private static final Logger log = LoggerFactory.getLogger(PatientRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PatientRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateNextPatientCode(Long hospitalId) {

        String sql = """
                SELECT COUNT(*)
                FROM patients
                WHERE hospital_id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, hospitalId);

        int next = count == null ? 1 : count + 1;

        return String.format("PAT-%06d", next);
    }

    public Long createPatient(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long createdBy,
            String patientCode,
            CreatePatientRequest request
    ) {

        log.debug("Creating patient. tenantId={} hospitalId={} branchId={}",
                tenantId, hospitalId, branchId);

        String fullName = buildFullName(request.firstName(), request.lastName());

        String sql = """
                INSERT INTO patients (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    patient_code,
                    first_name,
                    last_name,
                    full_name,
                    gender,
                    date_of_birth,
                    age_years,
                    blood_group,
                    phone,
                    alternate_phone,
                    email,
                    address,
                    city,
                    state,
                    country,
                    pincode,
                    emergency_contact_name,
                    emergency_contact_phone,
                    emergency_contact_relation,
                    source_channel,
                    payer_type,
                    status,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, NOW(), 0
                )
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                patientCode,
                request.firstName(),
                request.lastName(),
                fullName,
                request.gender(),
                request.dateOfBirth(),
                request.ageYears(),
                request.bloodGroup(),
                request.phone(),
                request.alternatePhone(),
                request.email(),
                request.address(),
                request.city(),
                request.state(),
                request.country() == null || request.country().isBlank() ? "India" : request.country(),
                request.pincode(),
                request.emergencyContactName(),
                request.emergencyContactPhone(),
                request.emergencyContactRelation(),
                request.sourceChannel(),
                request.payerType(),
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int updatePatient(
            Long patientId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            UpdatePatientRequest request
    ) {

        log.debug("Updating patient. patientId={} tenantId={} hospitalId={}",
                patientId, tenantId, hospitalId);

        String fullName = buildFullName(request.firstName(), request.lastName());

        String sql = """
                UPDATE patients
                SET first_name = ?,
                    last_name = ?,
                    full_name = ?,
                    gender = ?,
                    date_of_birth = ?,
                    age_years = ?,
                    blood_group = ?,
                    phone = ?,
                    alternate_phone = ?,
                    email = ?,
                    address = ?,
                    city = ?,
                    state = ?,
                    country = ?,
                    pincode = ?,
                    emergency_contact_name = ?,
                    emergency_contact_phone = ?,
                    emergency_contact_relation = ?,
                    source_channel = ?,
                    payer_type = ?,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(
                sql,
                request.firstName(),
                request.lastName(),
                fullName,
                request.gender(),
                request.dateOfBirth(),
                request.ageYears(),
                request.bloodGroup(),
                request.phone(),
                request.alternatePhone(),
                request.email(),
                request.address(),
                request.city(),
                request.state(),
                request.country() == null || request.country().isBlank() ? "India" : request.country(),
                request.pincode(),
                request.emergencyContactName(),
                request.emergencyContactPhone(),
                request.emergencyContactRelation(),
                request.sourceChannel(),
                request.payerType(),
                updatedBy,
                patientId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public List<PatientRecord> findPatients(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String query,
            String status,
            int limit,
            int offset
    ) {

        log.debug("Fetching patients. tenantId={} hospitalId={} branchId={} query={} status={}",
                tenantId, hospitalId, branchId, query, status);

        String searchValue = query == null || query.isBlank() ? null : "%" + query.trim() + "%";
        String statusValue = status == null || status.isBlank() ? null : status.trim();

        String sql = """
                SELECT
                    id,
                    tenant_id,
                    hospital_id,
                    branch_id,
                    patient_code,
                    first_name,
                    last_name,
                    full_name,
                    gender,
                    date_of_birth,
                    age_years,
                    blood_group,
                    phone,
                    alternate_phone,
                    email,
                    address,
                    city,
                    state,
                    country,
                    pincode,
                    emergency_contact_name,
                    emergency_contact_phone,
                    emergency_contact_relation,
                    source_channel,
                    payer_type,
                    status,
                    created_at,
                    updated_at
                FROM patients
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                  AND (? IS NULL OR status = ?)
                  AND (
                        ? IS NULL
                        OR full_name LIKE ?
                        OR phone LIKE ?
                        OR patient_code LIKE ?
                        OR email LIKE ?
                  )
                ORDER BY created_at DESC, id DESC
                LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                this::mapPatient,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                statusValue,
                statusValue,
                searchValue,
                searchValue,
                searchValue,
                searchValue,
                searchValue,
                limit,
                offset
        );
    }

    public Optional<PatientRecord> findById(Long patientId, Long tenantId, Long hospitalId, Long branchId) {

        String sql = """
                SELECT
                    id,
                    tenant_id,
                    hospital_id,
                    branch_id,
                    patient_code,
                    first_name,
                    last_name,
                    full_name,
                    gender,
                    date_of_birth,
                    age_years,
                    blood_group,
                    phone,
                    alternate_phone,
                    email,
                    address,
                    city,
                    state,
                    country,
                    pincode,
                    emergency_contact_name,
                    emergency_contact_phone,
                    emergency_contact_relation,
                    source_channel,
                    payer_type,
                    status,
                    created_at,
                    updated_at
                FROM patients
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                LIMIT 1
                """;

        List<PatientRecord> result = jdbcTemplate.query(
                sql,
                this::mapPatient,
                patientId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public boolean existsByPhone(Long tenantId, Long hospitalId, String phone) {

        String sql = """
                SELECT COUNT(*)
                FROM patients
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND phone = ?
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, hospitalId, phone);

        return count != null && count > 0;
    }

    public boolean existsByPhoneExcludingId(Long tenantId, Long hospitalId, String phone, Long patientId) {

        String sql = """
                SELECT COUNT(*)
                FROM patients
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND phone = ?
                  AND id <> ?
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, hospitalId, phone, patientId);

        return count != null && count > 0;
    }

    public int softDelete(Long patientId, Long tenantId, Long hospitalId, Long branchId, Long updatedBy) {

        String sql = """
                UPDATE patients
                SET status = 'INACTIVE',
                    is_deleted = 1,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, updatedBy, patientId, tenantId, hospitalId, branchId, branchId);
    }

    private PatientRecord mapPatient(ResultSet rs, int rowNum) throws SQLException {

        return new PatientRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                rs.getString("patient_code"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("full_name"),
                rs.getString("gender"),
                rs.getDate("date_of_birth") == null ? null : rs.getDate("date_of_birth").toLocalDate(),
                getNullableInteger(rs, "age_years"),
                rs.getString("blood_group"),
                rs.getString("phone"),
                rs.getString("alternate_phone"),
                rs.getString("email"),
                rs.getString("address"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("country"),
                rs.getString("pincode"),
                rs.getString("emergency_contact_name"),
                rs.getString("emergency_contact_phone"),
                rs.getString("emergency_contact_relation"),
                rs.getString("source_channel"),
                rs.getString("payer_type"),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private String buildFullName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName.trim();
        }
        return firstName.trim() + " " + lastName.trim();
    }

    public record PatientRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String patientCode,
            String firstName,
            String lastName,
            String fullName,
            String gender,
            java.time.LocalDate dateOfBirth,
            Integer ageYears,
            String bloodGroup,
            String phone,
            String alternatePhone,
            String email,
            String address,
            String city,
            String state,
            String country,
            String pincode,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelation,
            String sourceChannel,
            String payerType,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }
}