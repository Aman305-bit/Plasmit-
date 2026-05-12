package com.plasmit.appointment.hospital.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.plasmit.appointment.hospital.service.AppointmentService.CreateAppointmentRequest;
import com.plasmit.appointment.hospital.service.AppointmentService.RescheduleRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AppointmentRepository {

    private static final Logger log = LoggerFactory.getLogger(AppointmentRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public AppointmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateNextAppointmentCode(Long hospitalId) {

        String sql = """
                SELECT COUNT(*)
                FROM appointments
                WHERE hospital_id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, hospitalId);
        int next = count == null ? 1 : count + 1;

        return String.format("APT-%06d", next);
    }

    public boolean patientExists(Long tenantId, Long hospitalId, Long patientId) {

        String sql = """
                SELECT COUNT(*)
                FROM patients
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, patientId, tenantId, hospitalId);
        return count != null && count > 0;
    }

    public boolean doctorExists(Long tenantId, Long hospitalId, Long doctorId) {

        String sql = """
                SELECT COUNT(*)
                FROM doctors
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, doctorId, tenantId, hospitalId);
        return count != null && count > 0;
    }

    public boolean doctorAvailable(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long doctorId,
            String dayOfWeek,
            LocalTime appointmentTime
    ) {

        String sql = """
                SELECT COUNT(*)
                FROM doctor_availability
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND doctor_id = ?
                  AND day_of_week = ?
                  AND is_available = 1
                  AND start_time <= ?
                  AND end_time > ?
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                doctorId,
                dayOfWeek,
                appointmentTime,
                appointmentTime
        );

        return count != null && count > 0;
    }

    public boolean slotAlreadyBooked(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long doctorId,
            LocalDate date,
            LocalTime time,
            Long excludeAppointmentId
    ) {

        String sql = """
                SELECT COUNT(*)
                FROM appointments
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND doctor_id = ?
                  AND appointment_date = ?
                  AND appointment_time = ?
                  AND status NOT IN ('CANCELLED')
                  AND is_deleted = 0
                  AND (? IS NULL OR id <> ?)
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                doctorId,
                date,
                time,
                excludeAppointmentId,
                excludeAppointmentId
        );

        return count != null && count > 0;
    }

    public Long createAppointment(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            Long createdBy,
            String appointmentCode,
            CreateAppointmentRequest request,
            LocalTime endTime
    ) {

        log.debug("Creating appointment. tenantId={} hospitalId={} branchId={} patientId={} doctorId={}",
                tenantId, hospitalId, branchId, request.patientId(), request.doctorId());

        String sql = """
                INSERT INTO appointments (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    department_id,
                    appointment_code,
                    patient_id,
                    doctor_id,
                    appointment_date,
                    appointment_time,
                    appointment_end_time,
                    appointment_type,
                    source_channel,
                    payer_type,
                    reason,
                    notes,
                    status,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'BOOKED', ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                departmentId,
                appointmentCode,
                request.patientId(),
                request.doctorId(),
                request.appointmentDate(),
                request.appointmentTime(),
                endTime,
                emptyDefault(request.appointmentType(), "OPD_CONSULTATION"),
                emptyDefault(request.sourceChannel(), "WALK_IN"),
                emptyDefault(request.payerType(), "SELF_PAY"),
                request.reason(),
                request.notes(),
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<AppointmentRecord> findAppointments(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            LocalDate date,
            String status,
            Long doctorId,
            Long patientId,
            int limit,
            int offset
    ) {

        String sql = """
                SELECT
                    a.id,
                    a.tenant_id,
                    a.hospital_id,
                    a.branch_id,
                    a.department_id,
                    a.appointment_code,
                    a.patient_id,
                    p.full_name AS patient_name,
                    p.phone AS patient_phone,
                    a.doctor_id,
                    d.full_name AS doctor_name,
                    d.specialization AS doctor_specialization,
                    a.appointment_date,
                    a.appointment_time,
                    a.appointment_end_time,
                    a.appointment_type,
                    a.source_channel,
                    a.payer_type,
                    a.reason,
                    a.notes,
                    a.status,
                    a.check_in_time,
                    a.completed_time,
                    a.cancelled_time,
                    a.cancellation_reason,
                    a.created_at,
                    a.updated_at
                FROM appointments a
                INNER JOIN patients p
                    ON p.id = a.patient_id
                    AND p.tenant_id = a.tenant_id
                    AND p.hospital_id = a.hospital_id
                INNER JOIN doctors d
                    ON d.id = a.doctor_id
                    AND d.tenant_id = a.tenant_id
                    AND d.hospital_id = a.hospital_id
                WHERE a.tenant_id = ?
                  AND a.hospital_id = ?
                  AND (? IS NULL OR a.branch_id = ?)
                  AND (? IS NULL OR a.appointment_date = ?)
                  AND (? IS NULL OR a.status = ?)
                  AND (? IS NULL OR a.doctor_id = ?)
                  AND (? IS NULL OR a.patient_id = ?)
                  AND a.is_deleted = 0
                ORDER BY a.appointment_date DESC, a.appointment_time DESC
                LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                this::mapAppointment,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                date,
                date,
                status,
                status,
                doctorId,
                doctorId,
                patientId,
                patientId,
                limit,
                offset
        );
    }

    public Optional<AppointmentRecord> findById(Long appointmentId, Long tenantId, Long hospitalId, Long branchId) {

        List<AppointmentRecord> result = findAppointmentsById(appointmentId, tenantId, hospitalId, branchId);
        return result.stream().findFirst();
    }

    private List<AppointmentRecord> findAppointmentsById(Long appointmentId, Long tenantId, Long hospitalId, Long branchId) {

        String sql = """
                SELECT
                    a.id,
                    a.tenant_id,
                    a.hospital_id,
                    a.branch_id,
                    a.department_id,
                    a.appointment_code,
                    a.patient_id,
                    p.full_name AS patient_name,
                    p.phone AS patient_phone,
                    a.doctor_id,
                    d.full_name AS doctor_name,
                    d.specialization AS doctor_specialization,
                    a.appointment_date,
                    a.appointment_time,
                    a.appointment_end_time,
                    a.appointment_type,
                    a.source_channel,
                    a.payer_type,
                    a.reason,
                    a.notes,
                    a.status,
                    a.check_in_time,
                    a.completed_time,
                    a.cancelled_time,
                    a.cancellation_reason,
                    a.created_at,
                    a.updated_at
                FROM appointments a
                INNER JOIN patients p
                    ON p.id = a.patient_id
                    AND p.tenant_id = a.tenant_id
                    AND p.hospital_id = a.hospital_id
                INNER JOIN doctors d
                    ON d.id = a.doctor_id
                    AND d.tenant_id = a.tenant_id
                    AND d.hospital_id = a.hospital_id
                WHERE a.id = ?
                  AND a.tenant_id = ?
                  AND a.hospital_id = ?
                  AND (? IS NULL OR a.branch_id = ?)
                  AND a.is_deleted = 0
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, this::mapAppointment, appointmentId, tenantId, hospitalId, branchId, branchId);
    }

    public int updateStatus(
            Long appointmentId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            String oldStatus,
            String newStatus,
            String remarks
    ) {

        String timeColumn = switch (newStatus) {
            case "CHECKED_IN" -> "check_in_time = NOW(),";
            case "COMPLETED" -> "completed_time = NOW(),";
            case "CANCELLED" -> "cancelled_time = NOW(), cancellation_reason = ?,";
            default -> "";
        };

        if ("CANCELLED".equals(newStatus)) {
            String sql = """
                    UPDATE appointments
                    SET status = ?,
                        cancelled_time = NOW(),
                        cancellation_reason = ?,
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
                    newStatus,
                    remarks,
                    updatedBy,
                    appointmentId,
                    tenantId,
                    hospitalId,
                    branchId,
                    branchId
            );
        }

        String sql = """
                UPDATE appointments
                SET status = ?,
                    %s
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """.formatted(timeColumn);

        return jdbcTemplate.update(
                sql,
                newStatus,
                updatedBy,
                appointmentId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public int reschedule(
            Long appointmentId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            RescheduleRequest request,
            LocalTime endTime
    ) {

        String sql = """
                UPDATE appointments
                SET appointment_date = ?,
                    appointment_time = ?,
                    appointment_end_time = ?,
                    status = 'BOOKED',
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
                request.appointmentDate(),
                request.appointmentTime(),
                endTime,
                updatedBy,
                appointmentId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public void saveStatusHistory(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long appointmentId,
            String oldStatus,
            String newStatus,
            String remarks,
            Long changedBy
    ) {

        String sql = """
                INSERT INTO appointment_status_history (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    appointment_id,
                    old_status,
                    new_status,
                    remarks,
                    changed_by,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                appointmentId,
                oldStatus,
                newStatus,
                remarks,
                changedBy
        );
    }

    public List<SlotResponse> findBookedSlots(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long doctorId,
            LocalDate date
    ) {

        String sql = """
                SELECT appointment_time, appointment_end_time, status
                FROM appointments
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND doctor_id = ?
                  AND appointment_date = ?
                  AND status NOT IN ('CANCELLED')
                  AND is_deleted = 0
                ORDER BY appointment_time ASC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SlotResponse(
                        rs.getTime("appointment_time").toLocalTime(),
                        rs.getTime("appointment_end_time") == null ? null : rs.getTime("appointment_end_time").toLocalTime(),
                        rs.getString("status")
                ),
                tenantId,
                hospitalId,
                branchId,
                branchId,
                doctorId,
                date
        );
    }

    private AppointmentRecord mapAppointment(ResultSet rs, int rowNum) throws SQLException {

        return new AppointmentRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                getNullableLong(rs, "department_id"),
                rs.getString("appointment_code"),
                rs.getLong("patient_id"),
                rs.getString("patient_name"),
                rs.getString("patient_phone"),
                rs.getLong("doctor_id"),
                rs.getString("doctor_name"),
                rs.getString("doctor_specialization"),
                rs.getDate("appointment_date").toLocalDate(),
                rs.getTime("appointment_time").toLocalTime(),
                rs.getTime("appointment_end_time") == null ? null : rs.getTime("appointment_end_time").toLocalTime(),
                rs.getString("appointment_type"),
                rs.getString("source_channel"),
                rs.getString("payer_type"),
                rs.getString("reason"),
                rs.getString("notes"),
                rs.getString("status"),
                rs.getTimestamp("check_in_time") == null ? null : rs.getTimestamp("check_in_time").toLocalDateTime().toString(),
                rs.getTimestamp("completed_time") == null ? null : rs.getTimestamp("completed_time").toLocalDateTime().toString(),
                rs.getTimestamp("cancelled_time") == null ? null : rs.getTimestamp("cancelled_time").toLocalDateTime().toString(),
                rs.getString("cancellation_reason"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String emptyDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record AppointmentRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            String appointmentCode,
            Long patientId,
            String patientName,
            String patientPhone,
            Long doctorId,
            String doctorName,
            String doctorSpecialization,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            LocalTime appointmentEndTime,
            String appointmentType,
            String sourceChannel,
            String payerType,
            String reason,
            String notes,
            String status,
            String checkInTime,
            String completedTime,
            String cancelledTime,
            String cancellationReason,
            String createdAt,
            String updatedAt
    ) {
    }

    public record SlotResponse(
            LocalTime appointmentTime,
            LocalTime appointmentEndTime,
            String status
    ) {
    }
}