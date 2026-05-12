package com.plasmit.doctor.hospital.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.plasmit.doctor.hospital.service.DoctorService.AvailabilityItemRequest;
import com.plasmit.doctor.hospital.service.DoctorService.CreateDoctorRequest;
import com.plasmit.doctor.hospital.service.DoctorService.UpdateDoctorRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DoctorRepository {

    private static final Logger log = LoggerFactory.getLogger(DoctorRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public DoctorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateNextDoctorCode(Long hospitalId) {

        String sql = """
                SELECT COUNT(*)
                FROM doctors
                WHERE hospital_id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, hospitalId);
        int next = count == null ? 1 : count + 1;

        return String.format("DOC-%06d", next);
    }

    public Long createDoctor(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            Long createdBy,
            String doctorCode,
            CreateDoctorRequest request
    ) {

        log.debug("Creating doctor. tenantId={} hospitalId={} branchId={}",
                tenantId, hospitalId, branchId);

        String fullName = buildFullName(request.firstName(), request.lastName());

        String sql = """
                INSERT INTO doctors (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    department_id,
                    doctor_code,
                    first_name,
                    last_name,
                    full_name,
                    gender,
                    date_of_birth,
                    phone,
                    alternate_phone,
                    email,
                    qualification,
                    specialization,
                    registration_number,
                    experience_years,
                    consultation_fee,
                    followup_fee,
                    emergency_fee,
                    consultation_duration_minutes,
                    address,
                    city,
                    state,
                    country,
                    pincode,
                    profile_photo_url,
                    notes,
                    status,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, NOW(), 0
                )
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                departmentId,
                doctorCode,
                request.firstName(),
                request.lastName(),
                fullName,
                request.gender(),
                request.dateOfBirth(),
                request.phone(),
                request.alternatePhone(),
                request.email(),
                request.qualification(),
                request.specialization(),
                request.registrationNumber(),
                request.experienceYears(),
                request.consultationFee(),
                request.followupFee(),
                request.emergencyFee(),
                request.consultationDurationMinutes(),
                request.address(),
                request.city(),
                request.state(),
                request.country() == null || request.country().isBlank() ? "India" : request.country(),
                request.pincode(),
                request.profilePhotoUrl(),
                request.notes(),
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int updateDoctor(
            Long doctorId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            UpdateDoctorRequest request
    ) {

        log.debug("Updating doctor. doctorId={} tenantId={} hospitalId={}",
                doctorId, tenantId, hospitalId);

        String fullName = buildFullName(request.firstName(), request.lastName());

        String sql = """
                UPDATE doctors
                SET first_name = ?,
                    last_name = ?,
                    full_name = ?,
                    gender = ?,
                    date_of_birth = ?,
                    phone = ?,
                    alternate_phone = ?,
                    email = ?,
                    qualification = ?,
                    specialization = ?,
                    registration_number = ?,
                    experience_years = ?,
                    consultation_fee = ?,
                    followup_fee = ?,
                    emergency_fee = ?,
                    consultation_duration_minutes = ?,
                    address = ?,
                    city = ?,
                    state = ?,
                    country = ?,
                    pincode = ?,
                    profile_photo_url = ?,
                    notes = ?,
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
                request.phone(),
                request.alternatePhone(),
                request.email(),
                request.qualification(),
                request.specialization(),
                request.registrationNumber(),
                request.experienceYears(),
                request.consultationFee(),
                request.followupFee(),
                request.emergencyFee(),
                request.consultationDurationMinutes(),
                request.address(),
                request.city(),
                request.state(),
                request.country() == null || request.country().isBlank() ? "India" : request.country(),
                request.pincode(),
                request.profilePhotoUrl(),
                request.notes(),
                updatedBy,
                doctorId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public List<DoctorRecord> findDoctors(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String query,
            String status,
            int limit,
            int offset
    ) {

        log.debug("Fetching doctors. tenantId={} hospitalId={} branchId={} query={} status={}",
                tenantId, hospitalId, branchId, query, status);

        String searchValue = query == null || query.isBlank() ? null : "%" + query.trim() + "%";
        String statusValue = status == null || status.isBlank() ? null : status.trim();

        String sql = """
                SELECT
                    id,
                    tenant_id,
                    hospital_id,
                    branch_id,
                    department_id,
                    doctor_code,
                    first_name,
                    last_name,
                    full_name,
                    gender,
                    date_of_birth,
                    phone,
                    alternate_phone,
                    email,
                    qualification,
                    specialization,
                    registration_number,
                    experience_years,
                    consultation_fee,
                    followup_fee,
                    emergency_fee,
                    consultation_duration_minutes,
                    address,
                    city,
                    state,
                    country,
                    pincode,
                    profile_photo_url,
                    notes,
                    status,
                    created_at,
                    updated_at
                FROM doctors
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                  AND (? IS NULL OR status = ?)
                  AND (
                        ? IS NULL
                        OR full_name LIKE ?
                        OR phone LIKE ?
                        OR doctor_code LIKE ?
                        OR specialization LIKE ?
                        OR registration_number LIKE ?
                  )
                ORDER BY created_at DESC, id DESC
                LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                this::mapDoctor,
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
                searchValue,
                limit,
                offset
        );
    }

    public Optional<DoctorRecord> findById(Long doctorId, Long tenantId, Long hospitalId, Long branchId) {

        String sql = """
                SELECT
                    id,
                    tenant_id,
                    hospital_id,
                    branch_id,
                    department_id,
                    doctor_code,
                    first_name,
                    last_name,
                    full_name,
                    gender,
                    date_of_birth,
                    phone,
                    alternate_phone,
                    email,
                    qualification,
                    specialization,
                    registration_number,
                    experience_years,
                    consultation_fee,
                    followup_fee,
                    emergency_fee,
                    consultation_duration_minutes,
                    address,
                    city,
                    state,
                    country,
                    pincode,
                    profile_photo_url,
                    notes,
                    status,
                    created_at,
                    updated_at
                FROM doctors
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                LIMIT 1
                """;

        List<DoctorRecord> result = jdbcTemplate.query(
                sql,
                this::mapDoctor,
                doctorId,
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
                FROM doctors
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND phone = ?
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, hospitalId, phone);
        return count != null && count > 0;
    }

    public boolean existsByRegistration(Long tenantId, Long hospitalId, String registrationNumber) {

        if (registrationNumber == null || registrationNumber.isBlank()) {
            return false;
        }

        String sql = """
                SELECT COUNT(*)
                FROM doctors
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND registration_number = ?
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, hospitalId, registrationNumber);
        return count != null && count > 0;
    }

    public boolean existsByPhoneExcludingId(Long tenantId, Long hospitalId, String phone, Long doctorId) {

        String sql = """
                SELECT COUNT(*)
                FROM doctors
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND phone = ?
                  AND id <> ?
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, hospitalId, phone, doctorId);
        return count != null && count > 0;
    }

    public boolean existsByRegistrationExcludingId(
            Long tenantId,
            Long hospitalId,
            String registrationNumber,
            Long doctorId
    ) {

        if (registrationNumber == null || registrationNumber.isBlank()) {
            return false;
        }

        String sql = """
                SELECT COUNT(*)
                FROM doctors
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND registration_number = ?
                  AND id <> ?
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, hospitalId, registrationNumber, doctorId);
        return count != null && count > 0;
    }

    public int softDelete(Long doctorId, Long tenantId, Long hospitalId, Long branchId, Long updatedBy) {

        String sql = """
                UPDATE doctors
                SET status = 'ARCHIVED',
                    is_deleted = 1,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, updatedBy, doctorId, tenantId, hospitalId, branchId, branchId);
    }

    public int updateConsultationFee(
            Long doctorId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            BigDecimal consultationFee,
            BigDecimal followupFee,
            BigDecimal emergencyFee
    ) {

        String sql = """
                UPDATE doctors
                SET consultation_fee = ?,
                    followup_fee = ?,
                    emergency_fee = ?,
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
                consultationFee,
                followupFee,
                emergencyFee,
                updatedBy,
                doctorId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public List<AvailabilityRecord> findAvailability(
            Long doctorId,
            Long tenantId,
            Long hospitalId,
            Long branchId
    ) {

        String sql = """
                SELECT
                    id,
                    tenant_id,
                    hospital_id,
                    branch_id,
                    doctor_id,
                    day_of_week,
                    is_available,
                    start_time,
                    end_time,
                    break_start_time,
                    break_end_time,
                    slot_duration_minutes,
                    max_patients,
                    consultation_mode,
                    status
                FROM doctor_availability
                WHERE doctor_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                ORDER BY FIELD(day_of_week, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
                """;

        return jdbcTemplate.query(sql, this::mapAvailability, doctorId, tenantId, hospitalId, branchId, branchId);
    }

    public void upsertAvailability(
            Long doctorId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long userId,
            AvailabilityItemRequest item
    ) {

        String sql = """
                INSERT INTO doctor_availability (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    doctor_id,
                    day_of_week,
                    is_available,
                    start_time,
                    end_time,
                    break_start_time,
                    break_end_time,
                    slot_duration_minutes,
                    max_patients,
                    consultation_mode,
                    status,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, NOW(), 0)
                ON DUPLICATE KEY UPDATE
                    is_available = VALUES(is_available),
                    start_time = VALUES(start_time),
                    end_time = VALUES(end_time),
                    break_start_time = VALUES(break_start_time),
                    break_end_time = VALUES(break_end_time),
                    slot_duration_minutes = VALUES(slot_duration_minutes),
                    max_patients = VALUES(max_patients),
                    consultation_mode = VALUES(consultation_mode),
                    status = 'ACTIVE',
                    updated_by = VALUES(created_by),
                    updated_at = NOW(),
                    is_deleted = 0
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                doctorId,
                item.dayOfWeek(),
                item.isAvailable() ? 1 : 0,
                item.startTime(),
                item.endTime(),
                item.breakStartTime(),
                item.breakEndTime(),
                item.slotDurationMinutes(),
                item.maxPatients(),
                item.consultationMode(),
                userId
        );
    }

    private DoctorRecord mapDoctor(ResultSet rs, int rowNum) throws SQLException {

        return new DoctorRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                getNullableLong(rs, "department_id"),
                rs.getString("doctor_code"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("full_name"),
                rs.getString("gender"),
                rs.getDate("date_of_birth") == null ? null : rs.getDate("date_of_birth").toLocalDate(),
                rs.getString("phone"),
                rs.getString("alternate_phone"),
                rs.getString("email"),
                rs.getString("qualification"),
                rs.getString("specialization"),
                rs.getString("registration_number"),
                getNullableInteger(rs, "experience_years"),
                rs.getBigDecimal("consultation_fee"),
                rs.getBigDecimal("followup_fee"),
                rs.getBigDecimal("emergency_fee"),
                getNullableInteger(rs, "consultation_duration_minutes"),
                rs.getString("address"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("country"),
                rs.getString("pincode"),
                rs.getString("profile_photo_url"),
                rs.getString("notes"),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private AvailabilityRecord mapAvailability(ResultSet rs, int rowNum) throws SQLException {

        return new AvailabilityRecord(
                rs.getLong("id"),
                rs.getLong("doctor_id"),
                getNullableLong(rs, "branch_id"),
                rs.getString("day_of_week"),
                rs.getInt("is_available") == 1,
                rs.getTime("start_time") == null ? null : rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time") == null ? null : rs.getTime("end_time").toLocalTime(),
                rs.getTime("break_start_time") == null ? null : rs.getTime("break_start_time").toLocalTime(),
                rs.getTime("break_end_time") == null ? null : rs.getTime("break_end_time").toLocalTime(),
                getNullableInteger(rs, "slot_duration_minutes"),
                getNullableInteger(rs, "max_patients"),
                rs.getString("consultation_mode"),
                rs.getString("status")
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
        String prefix = "Dr. ";
        if (lastName == null || lastName.isBlank()) {
            return prefix + firstName.trim();
        }
        return prefix + firstName.trim() + " " + lastName.trim();
    }

    public record DoctorRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            String doctorCode,
            String firstName,
            String lastName,
            String fullName,
            String gender,
            LocalDate dateOfBirth,
            String phone,
            String alternatePhone,
            String email,
            String qualification,
            String specialization,
            String registrationNumber,
            Integer experienceYears,
            BigDecimal consultationFee,
            BigDecimal followupFee,
            BigDecimal emergencyFee,
            Integer consultationDurationMinutes,
            String address,
            String city,
            String state,
            String country,
            String pincode,
            String profilePhotoUrl,
            String notes,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record AvailabilityRecord(
            Long id,
            Long doctorId,
            Long branchId,
            String dayOfWeek,
            boolean available,
            LocalTime startTime,
            LocalTime endTime,
            LocalTime breakStartTime,
            LocalTime breakEndTime,
            Integer slotDurationMinutes,
            Integer maxPatients,
            String consultationMode,
            String status
    ) {
    }
}