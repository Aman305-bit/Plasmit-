package com.plasmit.prescription.hospital.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.plasmit.prescription.hospital.service.PrescriptionService.CreatePrescriptionRequest;
import com.plasmit.prescription.hospital.service.PrescriptionService.DiagnosisRequest;
import com.plasmit.prescription.hospital.service.PrescriptionService.MedicineRequest;
import com.plasmit.prescription.hospital.service.PrescriptionService.UpdatePrescriptionRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PrescriptionRepository {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PrescriptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateNextPrescriptionNumber(Long hospitalId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prescriptions WHERE hospital_id = ?",
                Integer.class,
                hospitalId
        );

        int next = count == null ? 1 : count + 1;
        return String.format("RX-%06d", next);
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

    public boolean appointmentExists(Long tenantId, Long hospitalId, Long patientId, Long doctorId, Long appointmentId) {
        if (appointmentId == null) {
            return true;
        }

        String sql = """
                SELECT COUNT(*)
                FROM appointments
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND patient_id = ?
                  AND doctor_id = ?
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                appointmentId,
                tenantId,
                hospitalId,
                patientId,
                doctorId
        );

        return count != null && count > 0;
    }

    public Long createPrescription(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            Long createdBy,
            String prescriptionNumber,
            CreatePrescriptionRequest request
    ) {

        log.debug("Creating prescription. tenantId={} hospitalId={} branchId={} patientId={} doctorId={}",
                tenantId, hospitalId, branchId, request.patientId(), request.doctorId());

        String sql = """
                INSERT INTO prescriptions (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    department_id,
                    prescription_number,
                    patient_id,
                    doctor_id,
                    appointment_id,
                    prescription_date,
                    chief_complaint,
                    clinical_notes,
                    diagnosis_summary,
                    advice_summary,
                    followup_date,
                    status,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURDATE(), ?, ?, ?, ?, ?, 'ACTIVE', ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                departmentId,
                prescriptionNumber,
                request.patientId(),
                request.doctorId(),
                request.appointmentId(),
                request.chiefComplaint(),
                request.clinicalNotes(),
                request.diagnosisSummary(),
                request.adviceSummary(),
                request.followupDate(),
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int updatePrescription(
            Long prescriptionId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            UpdatePrescriptionRequest request
    ) {

        String sql = """
                UPDATE prescriptions
                SET chief_complaint = ?,
                    clinical_notes = ?,
                    diagnosis_summary = ?,
                    advice_summary = ?,
                    followup_date = ?,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(
                sql,
                request.chiefComplaint(),
                request.clinicalNotes(),
                request.diagnosisSummary(),
                request.adviceSummary(),
                request.followupDate(),
                updatedBy,
                prescriptionId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public void replaceMedicines(
            Long prescriptionId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            List<MedicineRequest> medicines
    ) {
        jdbcTemplate.update(
                """
                UPDATE prescription_medicines
                SET is_deleted = 1
                WHERE prescription_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                """,
                prescriptionId,
                tenantId,
                hospitalId
        );

        if (medicines == null) {
            return;
        }

        int sort = 1;

        for (MedicineRequest medicine : medicines) {
            jdbcTemplate.update(
                    """
                    INSERT INTO prescription_medicines (
                        tenant_id,
                        hospital_id,
                        branch_id,
                        prescription_id,
                        medicine_name,
                        dosage,
                        frequency,
                        duration,
                        route,
                        timing,
                        instructions,
                        sort_order,
                        created_at,
                        is_deleted
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0)
                    """,
                    tenantId,
                    hospitalId,
                    branchId,
                    prescriptionId,
                    medicine.medicineName(),
                    medicine.dosage(),
                    medicine.frequency(),
                    medicine.duration(),
                    medicine.route(),
                    medicine.timing(),
                    medicine.instructions(),
                    sort++
            );
        }
    }

    public void replaceDiagnoses(
            Long prescriptionId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            List<DiagnosisRequest> diagnoses
    ) {
        jdbcTemplate.update(
                """
                UPDATE prescription_diagnoses
                SET is_deleted = 1
                WHERE prescription_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                """,
                prescriptionId,
                tenantId,
                hospitalId
        );

        if (diagnoses == null) {
            return;
        }

        int sort = 1;

        for (DiagnosisRequest diagnosis : diagnoses) {
            jdbcTemplate.update(
                    """
                    INSERT INTO prescription_diagnoses (
                        tenant_id,
                        hospital_id,
                        branch_id,
                        prescription_id,
                        diagnosis_name,
                        diagnosis_code,
                        notes,
                        sort_order,
                        created_at,
                        is_deleted
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0)
                    """,
                    tenantId,
                    hospitalId,
                    branchId,
                    prescriptionId,
                    diagnosis.diagnosisName(),
                    diagnosis.diagnosisCode(),
                    diagnosis.notes(),
                    sort++
            );
        }
    }

    public void replaceAdvice(
            Long prescriptionId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            List<String> adviceList
    ) {
        jdbcTemplate.update(
                """
                UPDATE prescription_advice
                SET is_deleted = 1
                WHERE prescription_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                """,
                prescriptionId,
                tenantId,
                hospitalId
        );

        if (adviceList == null) {
            return;
        }

        int sort = 1;

        for (String advice : adviceList) {
            if (advice == null || advice.isBlank()) {
                continue;
            }

            jdbcTemplate.update(
                    """
                    INSERT INTO prescription_advice (
                        tenant_id,
                        hospital_id,
                        branch_id,
                        prescription_id,
                        advice_text,
                        sort_order,
                        created_at,
                        is_deleted
                    )
                    VALUES (?, ?, ?, ?, ?, ?, NOW(), 0)
                    """,
                    tenantId,
                    hospitalId,
                    branchId,
                    prescriptionId,
                    advice,
                    sort++
            );
        }
    }

    public List<PrescriptionRecord> findPrescriptions(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long patientId,
            Long doctorId,
            Long appointmentId,
            String status,
            int limit,
            int offset
    ) {

        String sql = """
                SELECT
                    pr.id,
                    pr.tenant_id,
                    pr.hospital_id,
                    pr.branch_id,
                    pr.department_id,
                    pr.prescription_number,
                    pr.patient_id,
                    p.full_name AS patient_name,
                    p.phone AS patient_phone,
                    pr.doctor_id,
                    d.full_name AS doctor_name,
                    d.specialization AS doctor_specialization,
                    pr.appointment_id,
                    pr.prescription_date,
                    pr.chief_complaint,
                    pr.clinical_notes,
                    pr.diagnosis_summary,
                    pr.advice_summary,
                    pr.followup_date,
                    pr.status,
                    pr.created_at,
                    pr.updated_at
                FROM prescriptions pr
                INNER JOIN patients p
                    ON p.id = pr.patient_id
                    AND p.tenant_id = pr.tenant_id
                    AND p.hospital_id = pr.hospital_id
                INNER JOIN doctors d
                    ON d.id = pr.doctor_id
                    AND d.tenant_id = pr.tenant_id
                    AND d.hospital_id = pr.hospital_id
                WHERE pr.tenant_id = ?
                  AND pr.hospital_id = ?
                  AND (? IS NULL OR pr.branch_id = ?)
                  AND (? IS NULL OR pr.patient_id = ?)
                  AND (? IS NULL OR pr.doctor_id = ?)
                  AND (? IS NULL OR pr.appointment_id = ?)
                  AND (? IS NULL OR pr.status = ?)
                  AND pr.is_deleted = 0
                ORDER BY pr.created_at DESC, pr.id DESC
                LIMIT ? OFFSET ?
                """;

        String statusValue = status == null || status.isBlank() ? null : status.trim();

        return jdbcTemplate.query(
                sql,
                this::mapPrescription,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                patientId,
                patientId,
                doctorId,
                doctorId,
                appointmentId,
                appointmentId,
                statusValue,
                statusValue,
                limit,
                offset
        );
    }

    public Optional<PrescriptionRecord> findById(Long prescriptionId, Long tenantId, Long hospitalId, Long branchId) {

        String sql = """
                SELECT
                    pr.id,
                    pr.tenant_id,
                    pr.hospital_id,
                    pr.branch_id,
                    pr.department_id,
                    pr.prescription_number,
                    pr.patient_id,
                    p.full_name AS patient_name,
                    p.phone AS patient_phone,
                    pr.doctor_id,
                    d.full_name AS doctor_name,
                    d.specialization AS doctor_specialization,
                    pr.appointment_id,
                    pr.prescription_date,
                    pr.chief_complaint,
                    pr.clinical_notes,
                    pr.diagnosis_summary,
                    pr.advice_summary,
                    pr.followup_date,
                    pr.status,
                    pr.created_at,
                    pr.updated_at
                FROM prescriptions pr
                INNER JOIN patients p
                    ON p.id = pr.patient_id
                    AND p.tenant_id = pr.tenant_id
                    AND p.hospital_id = pr.hospital_id
                INNER JOIN doctors d
                    ON d.id = pr.doctor_id
                    AND d.tenant_id = pr.tenant_id
                    AND d.hospital_id = pr.hospital_id
                WHERE pr.id = ?
                  AND pr.tenant_id = ?
                  AND pr.hospital_id = ?
                  AND (? IS NULL OR pr.branch_id = ?)
                  AND pr.is_deleted = 0
                LIMIT 1
                """;

        List<PrescriptionRecord> result = jdbcTemplate.query(
                sql,
                this::mapPrescription,
                prescriptionId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public List<MedicineRecord> findMedicines(Long prescriptionId, Long tenantId, Long hospitalId) {
        String sql = """
                SELECT
                    id,
                    prescription_id,
                    medicine_name,
                    dosage,
                    frequency,
                    duration,
                    route,
                    timing,
                    instructions,
                    sort_order
                FROM prescription_medicines
                WHERE prescription_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND is_deleted = 0
                ORDER BY sort_order ASC, id ASC
                """;

        return jdbcTemplate.query(sql, this::mapMedicine, prescriptionId, tenantId, hospitalId);
    }

    public List<DiagnosisRecord> findDiagnoses(Long prescriptionId, Long tenantId, Long hospitalId) {
        String sql = """
                SELECT
                    id,
                    prescription_id,
                    diagnosis_name,
                    diagnosis_code,
                    notes,
                    sort_order
                FROM prescription_diagnoses
                WHERE prescription_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND is_deleted = 0
                ORDER BY sort_order ASC, id ASC
                """;

        return jdbcTemplate.query(sql, this::mapDiagnosis, prescriptionId, tenantId, hospitalId);
    }

    public List<AdviceRecord> findAdvice(Long prescriptionId, Long tenantId, Long hospitalId) {
        String sql = """
                SELECT
                    id,
                    prescription_id,
                    advice_text,
                    sort_order
                FROM prescription_advice
                WHERE prescription_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND is_deleted = 0
                ORDER BY sort_order ASC, id ASC
                """;

        return jdbcTemplate.query(sql, this::mapAdvice, prescriptionId, tenantId, hospitalId);
    }

    public int cancelPrescription(Long prescriptionId, Long tenantId, Long hospitalId, Long branchId, Long updatedBy) {
        String sql = """
                UPDATE prescriptions
                SET status = 'CANCELLED',
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, updatedBy, prescriptionId, tenantId, hospitalId, branchId, branchId);
    }

    private PrescriptionRecord mapPrescription(ResultSet rs, int rowNum) throws SQLException {
        return new PrescriptionRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                getNullableLong(rs, "department_id"),
                rs.getString("prescription_number"),
                rs.getLong("patient_id"),
                rs.getString("patient_name"),
                rs.getString("patient_phone"),
                rs.getLong("doctor_id"),
                rs.getString("doctor_name"),
                rs.getString("doctor_specialization"),
                getNullableLong(rs, "appointment_id"),
                rs.getDate("prescription_date").toLocalDate(),
                rs.getString("chief_complaint"),
                rs.getString("clinical_notes"),
                rs.getString("diagnosis_summary"),
                rs.getString("advice_summary"),
                rs.getDate("followup_date") == null ? null : rs.getDate("followup_date").toLocalDate(),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private MedicineRecord mapMedicine(ResultSet rs, int rowNum) throws SQLException {
        return new MedicineRecord(
                rs.getLong("id"),
                rs.getLong("prescription_id"),
                rs.getString("medicine_name"),
                rs.getString("dosage"),
                rs.getString("frequency"),
                rs.getString("duration"),
                rs.getString("route"),
                rs.getString("timing"),
                rs.getString("instructions"),
                rs.getInt("sort_order")
        );
    }

    private DiagnosisRecord mapDiagnosis(ResultSet rs, int rowNum) throws SQLException {
        return new DiagnosisRecord(
                rs.getLong("id"),
                rs.getLong("prescription_id"),
                rs.getString("diagnosis_name"),
                rs.getString("diagnosis_code"),
                rs.getString("notes"),
                rs.getInt("sort_order")
        );
    }

    private AdviceRecord mapAdvice(ResultSet rs, int rowNum) throws SQLException {
        return new AdviceRecord(
                rs.getLong("id"),
                rs.getLong("prescription_id"),
                rs.getString("advice_text"),
                rs.getInt("sort_order")
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public record PrescriptionRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            String prescriptionNumber,
            Long patientId,
            String patientName,
            String patientPhone,
            Long doctorId,
            String doctorName,
            String doctorSpecialization,
            Long appointmentId,
            LocalDate prescriptionDate,
            String chiefComplaint,
            String clinicalNotes,
            String diagnosisSummary,
            String adviceSummary,
            LocalDate followupDate,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record MedicineRecord(
            Long id,
            Long prescriptionId,
            String medicineName,
            String dosage,
            String frequency,
            String duration,
            String route,
            String timing,
            String instructions,
            Integer sortOrder
    ) {
    }

    public record DiagnosisRecord(
            Long id,
            Long prescriptionId,
            String diagnosisName,
            String diagnosisCode,
            String notes,
            Integer sortOrder
    ) {
    }

    public record AdviceRecord(
            Long id,
            Long prescriptionId,
            String adviceText,
            Integer sortOrder
    ) {
    }
}