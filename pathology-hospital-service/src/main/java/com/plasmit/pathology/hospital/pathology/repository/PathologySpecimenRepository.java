package com.plasmit.pathology.hospital.pathology.repository;

import com.plasmit.pathology.hospital.common.exception.ApiException;
import com.plasmit.pathology.hospital.pathology.dto.request.ResultEntryRequest;
import com.plasmit.pathology.hospital.pathology.dto.request.ResultParameterRequest;
import com.plasmit.pathology.hospital.pathology.dto.response.*;
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
public class PathologySpecimenRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PathologySpecimenRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void syncSpecimensFromDiagnosticOrders(Long tenantId,
                                                  Long hospitalId,
                                                  String branchId,
                                                  Long userId) {

        String sql = """
                INSERT INTO pathology_specimens (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    diagnostic_order_id,
                    diagnostic_order_line_id,
                    service_id,
                    specimen_no,
                    barcode_no,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    service_code,
                    service_name,
                    modality,
                    sample_type,
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
                    CONCAT('SPC-', l.id, '-', UNIX_TIMESTAMP()),
                    COALESCE(o.barcode_no, CONCAT('BC-PATH-', l.id, '-', UNIX_TIMESTAMP())),
                    o.patient_id,
                    o.patient_uhid,
                    o.patient_name,
                    l.service_code,
                    l.service_name,
                    l.modality,
                    CASE
                        WHEN l.modality = 'Hematology' THEN 'Blood'
                        WHEN l.modality = 'Biochemistry' THEN 'Serum'
                        ELSE 'Sample'
                    END,
                    o.priority,
                    CASE
                        WHEN o.status IN ('Cancelled','Rejected') THEN 'Rejected'
                        WHEN o.status = 'SamplePending' THEN 'SamplePending'
                        ELSE 'SamplePending'
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
                  AND l.department = 'Pathology'
                  AND o.is_deleted = 0
                  AND l.is_deleted = 0
                  AND NOT EXISTS (
                      SELECT 1
                      FROM pathology_specimens ps
                      WHERE ps.hospital_id = o.hospital_id
                        AND ps.diagnostic_order_line_id = l.id
                        AND ps.is_deleted = 0
                  )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId).addValue("userId", userId));
    }

    public Long countSpecimens(Long tenantId,
                               Long hospitalId,
                               String branchId,
                               LocalDate fromDate,
                               LocalDate toDate,
                               String status,
                               String modality,
                               String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM pathology_specimens
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

    public List<PathologySpecimenResponse> findSpecimens(Long tenantId,
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
                    specimen_no,
                    barcode_no,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    service_code,
                    service_name,
                    modality,
                    sample_type,
                    priority,
                    status,
                    result_status,
                    collected_at,
                    received_at,
                    rejected_at,
                    rejected_reason,
                    technical_verified_at,
                    clinical_verified_at,
                    created_at
                FROM pathology_specimens
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

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapSpecimen(rs));
    }

    public PathologySpecimenResponse findSpecimenById(Long tenantId,
                                                      Long hospitalId,
                                                      String branchId,
                                                      Long specimenId) {

        String sql = """
                SELECT
                    id,
                    diagnostic_order_id,
                    diagnostic_order_line_id,
                    service_id,
                    specimen_no,
                    barcode_no,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    service_code,
                    service_name,
                    modality,
                    sample_type,
                    priority,
                    status,
                    result_status,
                    collected_at,
                    received_at,
                    rejected_at,
                    rejected_reason,
                    technical_verified_at,
                    clinical_verified_at,
                    created_at
                FROM pathology_specimens
                WHERE id = :specimenId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<PathologySpecimenResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("specimenId", specimenId),
                (rs, rowNum) -> mapSpecimen(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Pathology specimen not found.");
        }

        return rows.get(0);
    }

    public void updateCollect(Long tenantId,
                              Long hospitalId,
                              String branchId,
                              Long specimenId,
                              Long userId,
                              String collectedByName) {

        String sql = """
                UPDATE pathology_specimens
                SET status = 'Collected',
                    collected_at = NOW(),
                    collected_by = :userId,
                    collected_by_name = :collectedByName,
                    updated_by = :userId,
                    updated_at = NOW()
                WHERE id = :specimenId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        int updated = jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("specimenId", specimenId)
                .addValue("userId", userId)
                .addValue("collectedByName", collectedByName));

        if (updated == 0) {
            throw ApiException.notFound("Pathology specimen not found.");
        }
    }

    public void updateReceive(Long tenantId,
                              Long hospitalId,
                              String branchId,
                              Long specimenId,
                              Long userId,
                              String receivedByName) {

        String sql = """
                UPDATE pathology_specimens
                SET status = 'Received',
                    received_at = NOW(),
                    received_by = :userId,
                    received_by_name = :receivedByName,
                    updated_by = :userId,
                    updated_at = NOW()
                WHERE id = :specimenId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        int updated = jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("specimenId", specimenId)
                .addValue("userId", userId)
                .addValue("receivedByName", receivedByName));

        if (updated == 0) {
            throw ApiException.notFound("Pathology specimen not found.");
        }
    }

    public void updateReject(Long tenantId,
                             Long hospitalId,
                             String branchId,
                             Long specimenId,
                             Long userId,
                             String reason) {

        String sql = """
                UPDATE pathology_specimens
                SET status = 'Rejected',
                    rejected_at = NOW(),
                    rejected_by = :userId,
                    rejected_reason = :reason,
                    updated_by = :userId,
                    updated_at = NOW()
                WHERE id = :specimenId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        int updated = jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("specimenId", specimenId)
                .addValue("userId", userId)
                .addValue("reason", reason));

        if (updated == 0) {
            throw ApiException.notFound("Pathology specimen not found.");
        }
    }

    public Long createResultEntry(Long tenantId,
                                  Long hospitalId,
                                  String branchId,
                                  Long specimenId,
                                  Long userId,
                                  ResultEntryRequest request) {

        String resultNo = "RES-" + System.currentTimeMillis();

        String sql = """
                INSERT INTO pathology_result_entries (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    specimen_id,
                    result_no,
                    result_status,
                    result_summary,
                    abnormal_flag,
                    critical_flag,
                    entered_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :specimenId,
                    :resultNo,
                    'Draft',
                    :resultSummary,
                    :abnormalFlag,
                    :criticalFlag,
                    :enteredBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("specimenId", specimenId)
                .addValue("resultNo", resultNo)
                .addValue("resultSummary", request.resultSummary())
                .addValue("abnormalFlag", Boolean.TRUE.equals(request.abnormalFlag()) ? 1 : 0)
                .addValue("criticalFlag", Boolean.TRUE.equals(request.criticalFlag()) ? 1 : 0)
                .addValue("enteredBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void createResultParameter(Long tenantId,
                                      Long hospitalId,
                                      String branchId,
                                      Long resultEntryId,
                                      Long specimenId,
                                      ResultParameterRequest parameter) {

        String sql = """
                INSERT INTO pathology_result_parameters (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    result_entry_id,
                    specimen_id,
                    parameter_code,
                    parameter_name,
                    result_value,
                    unit,
                    reference_range,
                    abnormal_flag,
                    critical_flag,
                    display_order
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :resultEntryId,
                    :specimenId,
                    :parameterCode,
                    :parameterName,
                    :resultValue,
                    :unit,
                    :referenceRange,
                    :abnormalFlag,
                    :criticalFlag,
                    :displayOrder
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("resultEntryId", resultEntryId)
                .addValue("specimenId", specimenId)
                .addValue("parameterCode", parameter.parameterCode())
                .addValue("parameterName", parameter.parameterName())
                .addValue("resultValue", parameter.resultValue())
                .addValue("unit", parameter.unit())
                .addValue("referenceRange", parameter.referenceRange())
                .addValue("abnormalFlag", Boolean.TRUE.equals(parameter.abnormalFlag()) ? 1 : 0)
                .addValue("criticalFlag", Boolean.TRUE.equals(parameter.criticalFlag()) ? 1 : 0)
                .addValue("displayOrder", parameter.displayOrder() == null ? 0 : parameter.displayOrder());

        jdbc.update(sql, params);
    }

    public void markResultEntered(Long tenantId,
                                  Long hospitalId,
                                  String branchId,
                                  Long specimenId,
                                  Long userId) {

        String sql = """
                UPDATE pathology_specimens
                SET status = 'ResultEntered',
                    result_status = 'Draft',
                    updated_by = :userId,
                    updated_at = NOW()
                WHERE id = :specimenId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("specimenId", specimenId)
                .addValue("userId", userId));
    }

    public ResultEntryResponse findLatestResultEntry(Long tenantId,
                                                     Long hospitalId,
                                                     String branchId,
                                                     Long specimenId) {

        String sql = """
                SELECT
                    id,
                    specimen_id,
                    result_no,
                    result_status,
                    result_summary,
                    abnormal_flag,
                    critical_flag,
                    entered_by,
                    entered_at
                FROM pathology_result_entries
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND specimen_id = :specimenId
                  AND is_deleted = 0
                ORDER BY id DESC
                LIMIT 1
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("specimenId", specimenId);

        List<ResultEntryResponse> rows = jdbc.query(sql, params, (rs, rowNum) -> {
            Long resultEntryId = rs.getLong("id");

            return new ResultEntryResponse(
                    resultEntryId,
                    rs.getLong("specimen_id"),
                    rs.getString("result_no"),
                    rs.getString("result_status"),
                    rs.getString("result_summary"),
                    rs.getInt("abnormal_flag") == 1,
                    rs.getInt("critical_flag") == 1,
                    rs.getObject("entered_by") == null ? null : rs.getLong("entered_by"),
                    rs.getTimestamp("entered_at") == null ? null : rs.getTimestamp("entered_at").toLocalDateTime(),
                    findParameters(tenantId, hospitalId, branchId, resultEntryId)
            );
        });

        if (rows.isEmpty()) {
            throw ApiException.notFound("Pathology result entry not found.");
        }

        return rows.get(0);
    }

    public List<ResultParameterResponse> findParameters(Long tenantId,
                                                        Long hospitalId,
                                                        String branchId,
                                                        Long resultEntryId) {

        String sql = """
                SELECT
                    id,
                    parameter_code,
                    parameter_name,
                    result_value,
                    unit,
                    reference_range,
                    abnormal_flag,
                    critical_flag,
                    display_order
                FROM pathology_result_parameters
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND result_entry_id = :resultEntryId
                  AND is_deleted = 0
                ORDER BY display_order ASC, id ASC
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("resultEntryId", resultEntryId);

        return jdbc.query(sql, params, (rs, rowNum) -> new ResultParameterResponse(
                rs.getLong("id"),
                rs.getString("parameter_code"),
                rs.getString("parameter_name"),
                rs.getString("result_value"),
                rs.getString("unit"),
                rs.getString("reference_range"),
                rs.getInt("abnormal_flag") == 1,
                rs.getInt("critical_flag") == 1,
                rs.getInt("display_order")
        ));
    }

    public VerificationResponse createVerification(Long tenantId,
                                                   Long hospitalId,
                                                   String branchId,
                                                   Long specimenId,
                                                   Long resultEntryId,
                                                   String verificationType,
                                                   Long userId,
                                                   String verifierName,
                                                   String remarks) {

        String sql = """
                INSERT INTO pathology_verifications (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    specimen_id,
                    result_entry_id,
                    verification_type,
                    verification_status,
                    verifier_user_id,
                    verifier_name,
                    remarks
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :specimenId,
                    :resultEntryId,
                    :verificationType,
                    'Verified',
                    :verifierUserId,
                    :verifierName,
                    :remarks
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("specimenId", specimenId)
                .addValue("resultEntryId", resultEntryId)
                .addValue("verificationType", verificationType)
                .addValue("verifierUserId", userId)
                .addValue("verifierName", verifierName)
                .addValue("remarks", remarks);

        jdbc.update(sql, params);

        return new VerificationResponse(
                specimenId,
                resultEntryId,
                verificationType,
                "Verified",
                userId,
                verifierName,
                remarks,
                LocalDateTime.now()
        );
    }

    public void updateVerificationStatus(Long tenantId,
                                         Long hospitalId,
                                         String branchId,
                                         Long specimenId,
                                         Long resultEntryId,
                                         String type,
                                         Long userId) {

        String specimenStatus = "Technical".equals(type) ? "TechnicalVerified" : "ClinicalVerified";
        String resultStatus = "Technical".equals(type) ? "TechnicalVerified" : "ClinicalVerified";
        String timeColumn = "Technical".equals(type) ? "technical_verified_at" : "clinical_verified_at";
        String byColumn = "Technical".equals(type) ? "technical_verified_by" : "clinical_verified_by";

        String sql = """
                UPDATE pathology_specimens
                SET status = :specimenStatus,
                    result_status = :resultStatus,
                    updated_by = :userId,
                    updated_at = NOW(),
                """ + timeColumn + " = NOW(), " + byColumn + " = :userId " + """
                WHERE id = :specimenId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("specimenId", specimenId)
                .addValue("specimenStatus", specimenStatus)
                .addValue("resultStatus", resultStatus)
                .addValue("userId", userId));

        String resultSql = """
                UPDATE pathology_result_entries
                SET result_status = :resultStatus,
                    updated_by = :userId,
                    updated_at = NOW()
                WHERE id = :resultEntryId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(resultSql, baseParams(tenantId, hospitalId, branchId)
                .addValue("resultEntryId", resultEntryId)
                .addValue("resultStatus", resultStatus)
                .addValue("userId", userId));
    }

    public void insertEvent(Long tenantId,
                            Long hospitalId,
                            String branchId,
                            Long specimenId,
                            String fromStatus,
                            String toStatus,
                            String eventType,
                            String reason,
                            String notes,
                            Long userId,
                            String role,
                            String requestId) {

        String sql = """
                INSERT INTO pathology_specimen_events (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    specimen_id,
                    from_status,
                    to_status,
                    event_type,
                    reason,
                    notes,
                    actor_user_id,
                    actor_role,
                    request_id
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :specimenId,
                    :fromStatus,
                    :toStatus,
                    :eventType,
                    :reason,
                    :notes,
                    :actorUserId,
                    :actorRole,
                    :requestId
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("specimenId", specimenId)
                .addValue("fromStatus", fromStatus)
                .addValue("toStatus", toStatus)
                .addValue("eventType", eventType)
                .addValue("reason", reason)
                .addValue("notes", notes)
                .addValue("actorUserId", userId)
                .addValue("actorRole", role)
                .addValue("requestId", requestId);

        jdbc.update(sql, params);
    }

    private PathologySpecimenResponse mapSpecimen(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PathologySpecimenResponse(
                rs.getLong("id"),
                rs.getLong("diagnostic_order_id"),
                rs.getObject("diagnostic_order_line_id") == null ? null : rs.getLong("diagnostic_order_line_id"),
                rs.getLong("service_id"),
                rs.getString("specimen_no"),
                rs.getString("barcode_no"),
                rs.getLong("patient_id"),
                rs.getString("patient_uhid"),
                rs.getString("patient_name"),
                rs.getString("service_code"),
                rs.getString("service_name"),
                rs.getString("modality"),
                rs.getString("sample_type"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getString("result_status"),
                rs.getTimestamp("collected_at") == null ? null : rs.getTimestamp("collected_at").toLocalDateTime(),
                rs.getTimestamp("received_at") == null ? null : rs.getTimestamp("received_at").toLocalDateTime(),
                rs.getTimestamp("rejected_at") == null ? null : rs.getTimestamp("rejected_at").toLocalDateTime(),
                rs.getString("rejected_reason"),
                rs.getTimestamp("technical_verified_at") == null ? null : rs.getTimestamp("technical_verified_at").toLocalDateTime(),
                rs.getTimestamp("clinical_verified_at") == null ? null : rs.getTimestamp("clinical_verified_at").toLocalDateTime(),
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
                        specimen_no LIKE :search
                        OR barcode_no LIKE :search
                        OR patient_uhid LIKE :search
                        OR patient_name LIKE :search
                        OR service_name LIKE :search
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