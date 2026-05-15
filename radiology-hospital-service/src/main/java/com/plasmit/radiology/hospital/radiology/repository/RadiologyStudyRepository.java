package com.plasmit.radiology.hospital.radiology.repository;

import com.plasmit.radiology.hospital.common.exception.ApiException;
import com.plasmit.radiology.hospital.radiology.dto.request.PacsMatchRequest;
import com.plasmit.radiology.hospital.radiology.dto.request.RadiologyScheduleRequest;
import com.plasmit.radiology.hospital.radiology.dto.request.SafetyChecklistRequest;
import com.plasmit.radiology.hospital.radiology.dto.response.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class RadiologyStudyRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public RadiologyStudyRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void syncRadiologyStudiesFromDiagnosticOrders(Long tenantId,
                                                         Long hospitalId,
                                                         String branchId,
                                                         Long userId) {

        String sql = """
                INSERT INTO radiology_studies (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    diagnostic_order_id,
                    diagnostic_order_line_id,
                    service_id,
                    study_uid,
                    accession_no,
                    modality,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    priority,
                    status,
                    created_by
                )
                SELECT
                    o.tenant_id,
                    o.hospital_id,
                    o.branch_id,
                    o.id,
                    l.id,
                    l.service_id,
                    CONCAT('STUDY-', l.id, '-', UNIX_TIMESTAMP()),
                    o.accession_no,
                    l.modality,
                    o.patient_id,
                    o.patient_uhid,
                    o.patient_name,
                    o.priority,
                    CASE
                        WHEN o.status = 'Scheduled' THEN 'Scheduled'
                        WHEN o.status IN ('Cancelled','Rejected') THEN 'Cancelled'
                        ELSE 'Ordered'
                    END,
                    :userId
                FROM diagnostic_orders o
                INNER JOIN diagnostic_order_lines l ON l.order_id = o.id
                WHERE o.tenant_id = :tenantId
                  AND o.hospital_id = :hospitalId
                  AND o.branch_id = :branchId
                  AND l.tenant_id = :tenantId
                  AND l.hospital_id = :hospitalId
                  AND l.branch_id = :branchId
                  AND l.department = 'Radiology'
                  AND o.is_deleted = 0
                  AND l.is_deleted = 0
                  AND NOT EXISTS (
                      SELECT 1
                      FROM radiology_studies rs
                      WHERE rs.hospital_id = o.hospital_id
                        AND rs.diagnostic_order_line_id = l.id
                        AND rs.is_deleted = 0
                  )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId).addValue("userId", userId));
    }

    public Long countStudies(Long tenantId,
                             Long hospitalId,
                             String branchId,
                             LocalDate fromDate,
                             LocalDate toDate,
                             String status,
                             String modality,
                             String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM radiology_studies
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = listParams(tenantId, hospitalId, branchId, fromDate, toDate, status, modality, search);
        appendFilters(sql, status, modality, search);

        Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0L : total;
    }

    public List<RadiologyStudyResponse> findStudies(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String status,
                                                    String modality,
                                                    String search,
                                                    Integer page,
                                                    Integer limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    diagnostic_order_id,
                    diagnostic_order_line_id,
                    service_id,
                    study_uid,
                    accession_no,
                    modality,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    priority,
                    status,
                    scheduled_at,
                    scheduled_room,
                    radiologist_id,
                    radiologist_name,
                    pacs_status,
                    viewer_url,
                    created_at
                FROM radiology_studies
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = listParams(tenantId, hospitalId, branchId, fromDate, toDate, status, modality, search);
        appendFilters(sql, status, modality, search);

        sql.append("""
                ORDER BY
                    CASE WHEN priority = 'STAT' THEN 1
                         WHEN priority = 'Urgent' THEN 2
                         ELSE 3
                    END,
                    created_at DESC
                LIMIT :limit OFFSET :offset
                """);

        params.addValue("limit", limit);
        params.addValue("offset", (page - 1) * limit);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapStudy(rs));
    }

    public RadiologyStudyResponse findStudyById(Long tenantId,
                                                Long hospitalId,
                                                String branchId,
                                                Long studyId) {

        String sql = """
                SELECT
                    id,
                    diagnostic_order_id,
                    diagnostic_order_line_id,
                    service_id,
                    study_uid,
                    accession_no,
                    modality,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    priority,
                    status,
                    scheduled_at,
                    scheduled_room,
                    radiologist_id,
                    radiologist_name,
                    pacs_status,
                    viewer_url,
                    created_at
                FROM radiology_studies
                WHERE id = :studyId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<RadiologyStudyResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("studyId", studyId),
                (rs, rowNum) -> mapStudy(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Radiology study not found.");
        }

        return rows.get(0);
    }

    public RadiologyScheduleResponse scheduleStudy(Long tenantId,
                                                   Long hospitalId,
                                                   String branchId,
                                                   Long studyId,
                                                   RadiologyScheduleRequest request,
                                                   Long userId) {

        findStudyById(tenantId, hospitalId, branchId, studyId);

        String insertSql = """
                INSERT INTO radiology_schedule_events (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    study_id,
                    scheduled_at,
                    scheduled_room,
                    radiologist_id,
                    radiologist_name,
                    notes,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :studyId,
                    :scheduledAt,
                    :scheduledRoom,
                    :radiologistId,
                    :radiologistName,
                    :notes,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("studyId", studyId)
                .addValue("scheduledAt", Timestamp.valueOf(request.scheduledAt()))
                .addValue("scheduledRoom", request.scheduledRoom())
                .addValue("radiologistId", request.radiologistId())
                .addValue("radiologistName", request.radiologistName())
                .addValue("notes", request.notes())
                .addValue("createdBy", userId);

        jdbc.update(insertSql, params);

        String updateSql = """
                UPDATE radiology_studies
                SET status = 'Scheduled',
                    scheduled_at = :scheduledAt,
                    scheduled_room = :scheduledRoom,
                    radiologist_id = :radiologistId,
                    radiologist_name = :radiologistName,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :studyId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params.addValue("updatedBy", userId));

        return new RadiologyScheduleResponse(
                studyId,
                "Scheduled",
                request.scheduledAt(),
                request.scheduledRoom(),
                request.radiologistId(),
                request.radiologistName()
        );
    }

    public SafetyChecklistResponse saveSafetyChecklist(Long tenantId,
                                                       Long hospitalId,
                                                       String branchId,
                                                       Long studyId,
                                                       SafetyChecklistRequest request,
                                                       Long userId) {

        findStudyById(tenantId, hospitalId, branchId, studyId);

        String sql = """
                INSERT INTO radiology_safety_checklists (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    study_id,
                    pregnancy_check,
                    contrast_allergy_check,
                    renal_function_check,
                    metal_implant_check,
                    consent_taken,
                    remarks,
                    status,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :studyId,
                    :pregnancyCheck,
                    :contrastAllergyCheck,
                    :renalFunctionCheck,
                    :metalImplantCheck,
                    :consentTaken,
                    :remarks,
                    'Completed',
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("studyId", studyId)
                .addValue("pregnancyCheck", request.pregnancyCheck())
                .addValue("contrastAllergyCheck", request.contrastAllergyCheck())
                .addValue("renalFunctionCheck", request.renalFunctionCheck())
                .addValue("metalImplantCheck", request.metalImplantCheck())
                .addValue("consentTaken", Boolean.TRUE.equals(request.consentTaken()) ? 1 : 0)
                .addValue("remarks", request.remarks())
                .addValue("createdBy", userId);

        jdbc.update(sql, params);

        String updateSql = """
                UPDATE radiology_studies
                SET status = 'ReadyForScan',
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :studyId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params.addValue("updatedBy", userId));

        return new SafetyChecklistResponse(studyId, "Completed", request.consentTaken());
    }

    public PacsMatchResponse matchPacs(Long tenantId,
                                       Long hospitalId,
                                       String branchId,
                                       Long studyId,
                                       PacsMatchRequest request,
                                       Long userId) {

        findStudyById(tenantId, hospitalId, branchId, studyId);

        String sql = """
                INSERT INTO radiology_pacs_matches (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    study_id,
                    pacs_study_uid,
                    pacs_patient_id,
                    pacs_modality,
                    match_status,
                    match_confidence,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :studyId,
                    :pacsStudyUid,
                    :pacsPatientId,
                    :pacsModality,
                    'Matched',
                    :matchConfidence,
                    :createdBy
                )
                """;

        double confidence = request.matchConfidence() == null ? 100.0 : request.matchConfidence();

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("studyId", studyId)
                .addValue("pacsStudyUid", request.pacsStudyUid())
                .addValue("pacsPatientId", request.pacsPatientId())
                .addValue("pacsModality", request.pacsModality())
                .addValue("matchConfidence", confidence)
                .addValue("createdBy", userId);

        jdbc.update(sql, params);

        String viewerUrl = "https://viewer.plasmit.local/radiology/studies/" + request.pacsStudyUid();

        String updateSql = """
                UPDATE radiology_studies
                SET pacs_status = 'Matched',
                    study_uid = :pacsStudyUid,
                    viewer_url = :viewerUrl,
                    status = 'Completed',
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :studyId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params
                .addValue("viewerUrl", viewerUrl)
                .addValue("updatedBy", userId));

        return new PacsMatchResponse(
                studyId,
                request.pacsStudyUid(),
                "Matched",
                confidence
        );
    }

    public ViewerLaunchResponse launchViewer(Long tenantId,
                                             Long hospitalId,
                                             String branchId,
                                             Long studyId,
                                             Long userId,
                                             String requestId) {

        RadiologyStudyResponse study = findStudyById(tenantId, hospitalId, branchId, studyId);

        if (study.viewerUrl() == null || study.viewerUrl().isBlank()) {
            throw ApiException.workflow("Viewer URL not available. PACS match required.");
        }

        String auditSql = """
                INSERT INTO radiology_viewer_launch_audits (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    study_id,
                    viewer_url,
                    launched_by,
                    request_id
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :studyId,
                    :viewerUrl,
                    :launchedBy,
                    :requestId
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("studyId", studyId)
                .addValue("viewerUrl", study.viewerUrl())
                .addValue("launchedBy", userId)
                .addValue("requestId", requestId);

        jdbc.update(auditSql, params);

        return new ViewerLaunchResponse(
                studyId,
                study.studyUid(),
                study.viewerUrl(),
                "15 minutes"
        );
    }

    private RadiologyStudyResponse mapStudy(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RadiologyStudyResponse(
                rs.getLong("id"),
                rs.getLong("diagnostic_order_id"),
                rs.getObject("diagnostic_order_line_id") == null ? null : rs.getLong("diagnostic_order_line_id"),
                rs.getLong("service_id"),
                rs.getString("study_uid"),
                rs.getString("accession_no"),
                rs.getString("modality"),
                rs.getLong("patient_id"),
                rs.getString("patient_uhid"),
                rs.getString("patient_name"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getTimestamp("scheduled_at") == null ? null : rs.getTimestamp("scheduled_at").toLocalDateTime(),
                rs.getString("scheduled_room"),
                rs.getObject("radiologist_id") == null ? null : rs.getLong("radiologist_id"),
                rs.getString("radiologist_name"),
                rs.getString("pacs_status"),
                rs.getString("viewer_url"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private void appendFilters(StringBuilder sql, String status, String modality, String search) {
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status ");
        }

        if (modality != null && !modality.isBlank()) {
            sql.append(" AND modality = :modality ");
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        accession_no LIKE :search
                        OR patient_uhid LIKE :search
                        OR patient_name LIKE :search
                        OR study_uid LIKE :search
                    )
                    """);
        }
    }

    private MapSqlParameterSource listParams(Long tenantId,
                                             Long hospitalId,
                                             String branchId,
                                             LocalDate fromDate,
                                             LocalDate toDate,
                                             String status,
                                             String modality,
                                             String search) {

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);

        if (status != null && !status.isBlank()) {
            params.addValue("status", status);
        }

        if (modality != null && !modality.isBlank()) {
            params.addValue("modality", modality);
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