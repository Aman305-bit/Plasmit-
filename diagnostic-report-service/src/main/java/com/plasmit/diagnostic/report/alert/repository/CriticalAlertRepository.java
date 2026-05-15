package com.plasmit.diagnostic.report.alert.repository;

import com.plasmit.diagnostic.report.alert.dto.request.*;
import com.plasmit.diagnostic.report.alert.dto.response.*;
import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.reports.dto.response.DiagnosticReportResponse;
import com.plasmit.diagnostic.report.reports.repository.DiagnosticReportRepository;
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
public class CriticalAlertRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DiagnosticReportRepository reportRepository;

    public CriticalAlertRepository(NamedParameterJdbcTemplate jdbc,
                                   DiagnosticReportRepository reportRepository) {
        this.jdbc = jdbc;
        this.reportRepository = reportRepository;
    }

    public DiagnosticReportResponse getReport(Long tenantId,
                                              Long hospitalId,
                                              String branchId,
                                              Long reportId) {
        return reportRepository.findReportById(tenantId, hospitalId, branchId, reportId);
    }

    public Long countAlerts(Long tenantId,
                            Long hospitalId,
                            String branchId,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String status,
                            String severity,
                            String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM diagnostic_critical_alerts
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = listParams(
                tenantId, hospitalId, branchId, fromDate, toDate, status, severity, search
        );

        appendFilters(sql, status, severity, search);

        Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0L : total;
    }

    public List<CriticalAlertResponse> findAlerts(Long tenantId,
                                                  Long hospitalId,
                                                  String branchId,
                                                  LocalDate fromDate,
                                                  LocalDate toDate,
                                                  String status,
                                                  String severity,
                                                  String search,
                                                  Integer page,
                                                  Integer limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_critical_alerts
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = listParams(
                tenantId, hospitalId, branchId, fromDate, toDate, status, severity, search
        );

        appendFilters(sql, status, severity, search);

        sql.append("""
                ORDER BY
                    CASE
                        WHEN alert_status = 'Open' THEN 1
                        WHEN alert_status = 'Escalated' THEN 2
                        WHEN alert_status = 'Acknowledged' THEN 3
                        ELSE 4
                    END,
                    due_at ASC,
                    created_at DESC
                LIMIT :limit OFFSET :offset
                """);

        params.addValue("limit", limit);
        params.addValue("offset", (page - 1) * limit);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapAlert(rs));
    }

    public CriticalAlertResponse findAlertById(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long alertId) {

        String sql = """
                SELECT *
                FROM diagnostic_critical_alerts
                WHERE id = :alertId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<CriticalAlertResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("alertId", alertId),
                (rs, rowNum) -> mapAlert(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Critical alert not found.");
        }

        return rows.get(0);
    }

    public CriticalAlertResponse createAlert(Long tenantId,
                                             Long hospitalId,
                                             String branchId,
                                             Long reportId,
                                             Long userId,
                                             DiagnosticReportResponse report,
                                             CreateCriticalAlertRequest request,
                                             String alertNo) {

        String sql = """
                INSERT INTO diagnostic_critical_alerts (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
                    alert_no,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    department,
                    modality,
                    service_name,
                    severity,
                    alert_status,
                    alert_message,
                    notified_to_user_id,
                    notified_to_name,
                    notified_to_role,
                    notified_to_mobile,
                    notified_to_email,
                    due_at,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportId,
                    :alertNo,
                    :patientId,
                    :patientUhid,
                    :patientName,
                    :department,
                    :modality,
                    :serviceName,
                    :severity,
                    'Open',
                    :alertMessage,
                    :notifiedToUserId,
                    :notifiedToName,
                    :notifiedToRole,
                    :notifiedToMobile,
                    :notifiedToEmail,
                    :dueAt,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("alertNo", alertNo)
                .addValue("patientId", report.patientId())
                .addValue("patientUhid", report.patientUhid())
                .addValue("patientName", report.patientName())
                .addValue("department", report.department())
                .addValue("modality", report.modality())
                .addValue("serviceName", report.serviceName())
                .addValue("severity", request.severity() == null || request.severity().isBlank() ? "Critical" : request.severity())
                .addValue("alertMessage", request.alertMessage())
                .addValue("notifiedToUserId", request.notifiedToUserId())
                .addValue("notifiedToName", request.notifiedToName())
                .addValue("notifiedToRole", request.notifiedToRole())
                .addValue("notifiedToMobile", request.notifiedToMobile())
                .addValue("notifiedToEmail", request.notifiedToEmail())
                .addValue("dueAt", request.dueAt() == null ? null : java.sql.Timestamp.valueOf(request.dueAt()))
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        Long alertId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return findAlertById(tenantId, hospitalId, branchId, alertId);
    }

    public CriticalAlertAcknowledgementResponse acknowledge(Long tenantId,
                                                            Long hospitalId,
                                                            String branchId,
                                                            CriticalAlertResponse alert,
                                                            Long userId,
                                                            AcknowledgeCriticalAlertRequest request) {

        String insertSql = """
                INSERT INTO diagnostic_critical_alert_acknowledgements (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    alert_id,
                    report_id,
                    acknowledged_by,
                    acknowledged_by_name,
                    acknowledgement_notes
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :alertId,
                    :reportId,
                    :acknowledgedBy,
                    :acknowledgedByName,
                    :acknowledgementNotes
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("alertId", alert.alertId())
                .addValue("reportId", alert.reportId())
                .addValue("acknowledgedBy", userId)
                .addValue("acknowledgedByName", request.acknowledgedByName())
                .addValue("acknowledgementNotes", request.acknowledgementNotes());

        jdbc.update(insertSql, params);

        String updateSql = """
                UPDATE diagnostic_critical_alerts
                SET alert_status = 'Acknowledged',
                    acknowledged_at = NOW(),
                    acknowledged_by = :acknowledgedBy,
                    acknowledged_by_name = :acknowledgedByName,
                    acknowledgement_notes = :acknowledgementNotes,
                    updated_by = :acknowledgedBy,
                    updated_at = NOW()
                WHERE id = :alertId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params);

        return new CriticalAlertAcknowledgementResponse(
                alert.alertId(),
                alert.reportId(),
                userId,
                request.acknowledgedByName(),
                request.acknowledgementNotes(),
                LocalDateTime.now()
        );
    }

    public CriticalAlertEscalationResponse escalate(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    CriticalAlertResponse alert,
                                                    Long userId,
                                                    EscalateCriticalAlertRequest request,
                                                    Integer escalationLevel) {

        String insertSql = """
                INSERT INTO diagnostic_critical_alert_escalations (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    alert_id,
                    report_id,
                    escalated_from_user_id,
                    escalated_to_user_id,
                    escalated_to_name,
                    escalated_to_role,
                    escalated_to_mobile,
                    escalated_to_email,
                    escalation_reason,
                    escalation_level
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :alertId,
                    :reportId,
                    :escalatedFromUserId,
                    :escalatedToUserId,
                    :escalatedToName,
                    :escalatedToRole,
                    :escalatedToMobile,
                    :escalatedToEmail,
                    :escalationReason,
                    :escalationLevel
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("alertId", alert.alertId())
                .addValue("reportId", alert.reportId())
                .addValue("escalatedFromUserId", userId)
                .addValue("escalatedToUserId", request.escalatedToUserId())
                .addValue("escalatedToName", request.escalatedToName())
                .addValue("escalatedToRole", request.escalatedToRole())
                .addValue("escalatedToMobile", request.escalatedToMobile())
                .addValue("escalatedToEmail", request.escalatedToEmail())
                .addValue("escalationReason", request.escalationReason())
                .addValue("escalationLevel", escalationLevel);

        jdbc.update(insertSql, params);

        String updateSql = """
                UPDATE diagnostic_critical_alerts
                SET alert_status = 'Escalated',
                    escalated_at = NOW(),
                    escalated_to_user_id = :escalatedToUserId,
                    escalated_to_name = :escalatedToName,
                    escalation_reason = :escalationReason,
                    updated_by = :escalatedFromUserId,
                    updated_at = NOW()
                WHERE id = :alertId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params);

        return new CriticalAlertEscalationResponse(
                alert.alertId(),
                alert.reportId(),
                userId,
                request.escalatedToUserId(),
                request.escalatedToName(),
                request.escalatedToRole(),
                request.escalationReason(),
                escalationLevel,
                LocalDateTime.now()
        );
    }

    public CriticalAlertResponse closeAlert(Long tenantId,
                                            Long hospitalId,
                                            String branchId,
                                            CriticalAlertResponse alert,
                                            Long userId,
                                            CloseCriticalAlertRequest request) {

        String sql = """
                UPDATE diagnostic_critical_alerts
                SET alert_status = 'Closed',
                    closed_at = NOW(),
                    closed_by = :closedBy,
                    close_notes = :closeNotes,
                    updated_by = :closedBy,
                    updated_at = NOW()
                WHERE id = :alertId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("alertId", alert.alertId())
                .addValue("closedBy", userId)
                .addValue("closeNotes", request.closeNotes()));

        return findAlertById(tenantId, hospitalId, branchId, alert.alertId());
    }

    public Integer nextEscalationLevel(Long tenantId,
                                       Long hospitalId,
                                       String branchId,
                                       Long alertId) {

        String sql = """
                SELECT COALESCE(MAX(escalation_level), 0) + 1
                FROM diagnostic_critical_alert_escalations
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND alert_id = :alertId
                """;

        Integer level = jdbc.queryForObject(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("alertId", alertId),
                Integer.class
        );

        return level == null ? 1 : level;
    }

    public void insertEvent(Long tenantId,
                            Long hospitalId,
                            String branchId,
                            Long alertId,
                            Long reportId,
                            String fromStatus,
                            String toStatus,
                            String eventType,
                            String reason,
                            String notes,
                            Long userId,
                            String role,
                            String requestId) {

        String sql = """
                INSERT INTO diagnostic_critical_alert_events (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    alert_id,
                    report_id,
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
                    :alertId,
                    :reportId,
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

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("alertId", alertId)
                .addValue("reportId", reportId)
                .addValue("fromStatus", fromStatus)
                .addValue("toStatus", toStatus)
                .addValue("eventType", eventType)
                .addValue("reason", reason)
                .addValue("notes", notes)
                .addValue("actorUserId", userId)
                .addValue("actorRole", role)
                .addValue("requestId", requestId));
    }

    public List<CriticalAlertEventResponse> findTimeline(Long tenantId,
                                                         Long hospitalId,
                                                         String branchId,
                                                         Long alertId) {

        String sql = """
                SELECT *
                FROM diagnostic_critical_alert_events
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND alert_id = :alertId
                ORDER BY created_at ASC
                """;

        return jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("alertId", alertId),
                (rs, rowNum) -> new CriticalAlertEventResponse(
                        rs.getLong("id"),
                        rs.getLong("alert_id"),
                        rs.getLong("report_id"),
                        rs.getString("from_status"),
                        rs.getString("to_status"),
                        rs.getString("event_type"),
                        rs.getString("reason"),
                        rs.getString("notes"),
                        rs.getObject("actor_user_id") == null ? null : rs.getLong("actor_user_id"),
                        rs.getString("actor_role"),
                        rs.getString("request_id"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                )
        );
    }

    private CriticalAlertResponse mapAlert(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new CriticalAlertResponse(
                rs.getLong("id"),
                rs.getLong("report_id"),
                rs.getString("alert_no"),
                rs.getLong("patient_id"),
                rs.getString("patient_uhid"),
                rs.getString("patient_name"),
                rs.getString("department"),
                rs.getString("modality"),
                rs.getString("service_name"),
                rs.getString("severity"),
                rs.getString("alert_status"),
                rs.getString("alert_message"),
                rs.getObject("notified_to_user_id") == null ? null : rs.getLong("notified_to_user_id"),
                rs.getString("notified_to_name"),
                rs.getString("notified_to_role"),
                rs.getString("notified_to_mobile"),
                rs.getString("notified_to_email"),
                rs.getTimestamp("due_at") == null ? null : rs.getTimestamp("due_at").toLocalDateTime(),
                rs.getTimestamp("acknowledged_at") == null ? null : rs.getTimestamp("acknowledged_at").toLocalDateTime(),
                rs.getObject("acknowledged_by") == null ? null : rs.getLong("acknowledged_by"),
                rs.getString("acknowledged_by_name"),
                rs.getString("acknowledgement_notes"),
                rs.getTimestamp("escalated_at") == null ? null : rs.getTimestamp("escalated_at").toLocalDateTime(),
                rs.getObject("escalated_to_user_id") == null ? null : rs.getLong("escalated_to_user_id"),
                rs.getString("escalated_to_name"),
                rs.getString("escalation_reason"),
                rs.getTimestamp("closed_at") == null ? null : rs.getTimestamp("closed_at").toLocalDateTime(),
                rs.getObject("closed_by") == null ? null : rs.getLong("closed_by"),
                rs.getString("close_notes"),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private void appendFilters(StringBuilder sql,
                               String status,
                               String severity,
                               String search) {

        if (status != null && !status.isBlank()) {
            sql.append(" AND alert_status = :status ");
        }

        if (severity != null && !severity.isBlank()) {
            sql.append(" AND severity = :severity ");
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        alert_no LIKE :search
                        OR patient_uhid LIKE :search
                        OR patient_name LIKE :search
                        OR service_name LIKE :search
                        OR notified_to_name LIKE :search
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
                                             String severity,
                                             String search) {

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);

        if (status != null && !status.isBlank()) {
            params.addValue("status", status);
        }

        if (severity != null && !severity.isBlank()) {
            params.addValue("severity", severity);
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