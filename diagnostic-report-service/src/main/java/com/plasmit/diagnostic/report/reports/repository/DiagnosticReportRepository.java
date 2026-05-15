package com.plasmit.diagnostic.report.reports.repository;

import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.reports.dto.request.*;
import com.plasmit.diagnostic.report.reports.dto.response.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Repository
public class DiagnosticReportRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DiagnosticReportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long countReports(Long tenantId,
                             Long hospitalId,
                             String branchId,
                             LocalDate fromDate,
                             LocalDate toDate,
                             String status,
                             String sourceType,
                             String department,
                             String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM diagnostic_reports
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = listParams(
                tenantId, hospitalId, branchId, fromDate, toDate,
                status, sourceType, department, search
        );

        appendFilters(sql, status, sourceType, department, search);

        Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0L : total;
    }

    public List<DiagnosticReportResponse> findReports(Long tenantId,
                                                      Long hospitalId,
                                                      String branchId,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,
                                                      String status,
                                                      String sourceType,
                                                      String department,
                                                      String search,
                                                      Integer page,
                                                      Integer limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_reports
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = listParams(
                tenantId, hospitalId, branchId, fromDate, toDate,
                status, sourceType, department, search
        );

        appendFilters(sql, status, sourceType, department, search);

        sql.append("""
                ORDER BY created_at DESC
                LIMIT :limit OFFSET :offset
                """);

        params.addValue("limit", limit);
        params.addValue("offset", (page - 1) * limit);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapReport(rs.getLong("id"), rs));
    }

    public Long createReport(Long tenantId,
                             Long hospitalId,
                             String branchId,
                             Long userId,
                             CreateDiagnosticReportRequest request,
                             String reportNo,
                             String initialStatus,
                             String expertStatus) {

        String sql = """
                INSERT INTO diagnostic_reports (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_no,
                    source_type,
                    source_id,
                    diagnostic_order_id,
                    diagnostic_order_line_id,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    department,
                    modality,
                    service_id,
                    service_code,
                    service_name,
                    report_status,
                    clinical_summary,
                    impression,
                    recommendation,
                    abnormal_flag,
                    critical_flag,
                    expert_review_required,
                    expert_review_status,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportNo,
                    :sourceType,
                    :sourceId,
                    :diagnosticOrderId,
                    :diagnosticOrderLineId,
                    :patientId,
                    :patientUhid,
                    :patientName,
                    :department,
                    :modality,
                    :serviceId,
                    :serviceCode,
                    :serviceName,
                    :reportStatus,
                    :clinicalSummary,
                    :impression,
                    :recommendation,
                    :abnormalFlag,
                    :criticalFlag,
                    :expertReviewRequired,
                    :expertReviewStatus,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportNo", reportNo)
                .addValue("sourceType", request.sourceType())
                .addValue("sourceId", request.sourceId())
                .addValue("diagnosticOrderId", request.diagnosticOrderId())
                .addValue("diagnosticOrderLineId", request.diagnosticOrderLineId())
                .addValue("patientId", request.patientId())
                .addValue("patientUhid", request.patientUhid())
                .addValue("patientName", request.patientName())
                .addValue("department", request.department())
                .addValue("modality", request.modality())
                .addValue("serviceId", request.serviceId())
                .addValue("serviceCode", request.serviceCode())
                .addValue("serviceName", request.serviceName())
                .addValue("reportStatus", initialStatus)
                .addValue("clinicalSummary", request.clinicalSummary())
                .addValue("impression", request.impression())
                .addValue("recommendation", request.recommendation())
                .addValue("abnormalFlag", Boolean.TRUE.equals(request.abnormalFlag()) ? 1 : 0)
                .addValue("criticalFlag", Boolean.TRUE.equals(request.criticalFlag()) ? 1 : 0)
                .addValue("expertReviewRequired", Boolean.TRUE.equals(request.expertReviewRequired()) ? 1 : 0)
                .addValue("expertReviewStatus", expertStatus)
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void createSection(Long tenantId,
                              Long hospitalId,
                              String branchId,
                              Long reportId,
                              ReportSectionRequest section) {

        String sql = """
                INSERT INTO diagnostic_report_sections (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
                    section_title,
                    section_content,
                    display_order
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportId,
                    :sectionTitle,
                    :sectionContent,
                    :displayOrder
                )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("sectionTitle", section.sectionTitle())
                .addValue("sectionContent", section.sectionContent())
                .addValue("displayOrder", section.displayOrder() == null ? 0 : section.displayOrder()));
    }

    public void createObservation(Long tenantId,
                                  Long hospitalId,
                                  String branchId,
                                  Long reportId,
                                  ReportObservationRequest observation) {

        String sql = """
                INSERT INTO diagnostic_report_observations (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
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
                    :reportId,
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

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("parameterCode", observation.parameterCode())
                .addValue("parameterName", observation.parameterName())
                .addValue("resultValue", observation.resultValue())
                .addValue("unit", observation.unit())
                .addValue("referenceRange", observation.referenceRange())
                .addValue("abnormalFlag", Boolean.TRUE.equals(observation.abnormalFlag()) ? 1 : 0)
                .addValue("criticalFlag", Boolean.TRUE.equals(observation.criticalFlag()) ? 1 : 0)
                .addValue("displayOrder", observation.displayOrder() == null ? 0 : observation.displayOrder()));
    }

    public DiagnosticReportResponse findReportById(Long tenantId,
                                                   Long hospitalId,
                                                   String branchId,
                                                   Long reportId) {

        String sql = """
                SELECT *
                FROM diagnostic_reports
                WHERE id = :reportId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<DiagnosticReportResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> mapReport(reportId, rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Diagnostic report not found.");
        }

        return rows.get(0);
    }

    public ExpertReviewResponse expertReview(Long tenantId,
                                             Long hospitalId,
                                             String branchId,
                                             Long reportId,
                                             Long userId,
                                             ExpertReviewRequest request) {

        String insertSql = """
                INSERT INTO diagnostic_report_expert_reviews (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
                    reviewer_user_id,
                    reviewer_name,
                    review_status,
                    comments
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportId,
                    :reviewerUserId,
                    :reviewerName,
                    :reviewStatus,
                    :comments
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("reviewerUserId", userId)
                .addValue("reviewerName", request.reviewerName())
                .addValue("reviewStatus", request.reviewStatus())
                .addValue("comments", request.comments());

        jdbc.update(insertSql, params);

        String reportStatus = "Approved".equals(request.reviewStatus())
                ? "ExpertReviewed"
                : "ExpertReviewPending";

        String updateSql = """
                UPDATE diagnostic_reports
                SET expert_review_status = :reviewStatus,
                    report_status = :reportStatus,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :reportId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params
                .addValue("reportStatus", reportStatus)
                .addValue("updatedBy", userId));

        return new ExpertReviewResponse(
                reportId,
                userId,
                request.reviewerName(),
                request.reviewStatus(),
                request.comments(),
                LocalDateTime.now()
        );
    }

    public ReportSignatureResponse signReport(Long tenantId,
                                              Long hospitalId,
                                              String branchId,
                                              Long reportId,
                                              Long userId,
                                              SignReportRequest request) {

        String hash = signatureHash(reportId, userId, request.signerName());

        String insertSql = """
                INSERT INTO diagnostic_report_signatures (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
                    signer_user_id,
                    signer_name,
                    signer_role,
                    signature_hash
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportId,
                    :signerUserId,
                    :signerName,
                    :signerRole,
                    :signatureHash
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("signerUserId", userId)
                .addValue("signerName", request.signerName())
                .addValue("signerRole", request.signerRole())
                .addValue("signatureHash", hash);

        jdbc.update(insertSql, params);

        String updateSql = """
                UPDATE diagnostic_reports
                SET report_status = 'Signed',
                    signed_at = NOW(),
                    signed_by = :signerUserId,
                    signed_by_name = :signerName,
                    updated_by = :signerUserId,
                    updated_at = NOW()
                WHERE id = :reportId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params);

        return new ReportSignatureResponse(
                reportId,
                userId,
                request.signerName(),
                request.signerRole(),
                hash,
                LocalDateTime.now()
        );
    }

    public ReportReleaseResponse releaseReport(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long reportId,
                                               Long userId,
                                               ReleaseReportRequest request) {

        String insertSql = """
                INSERT INTO diagnostic_report_release_logs (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
                    release_channel,
                    released_by,
                    remarks
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportId,
                    :releaseChannel,
                    :releasedBy,
                    :remarks
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("releaseChannel", request.releaseChannel())
                .addValue("releasedBy", userId)
                .addValue("remarks", request.remarks());

        jdbc.update(insertSql, params);

        String updateSql = """
                UPDATE diagnostic_reports
                SET report_status = 'Released',
                    released_at = NOW(),
                    released_by = :releasedBy,
                    updated_by = :releasedBy,
                    updated_at = NOW()
                WHERE id = :reportId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params);

        return new ReportReleaseResponse(
                reportId,
                request.releaseChannel(),
                userId,
                request.remarks(),
                LocalDateTime.now()
        );
    }

    public ReportAmendmentResponse amendReport(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long reportId,
                                               Long userId,
                                               AmendReportRequest request) {

        String amendmentNo = "AMD-" + System.currentTimeMillis();

        String insertSql = """
                INSERT INTO diagnostic_report_amendments (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
                    amendment_no,
                    amendment_reason,
                    amended_content,
                    amended_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportId,
                    :amendmentNo,
                    :amendmentReason,
                    :amendedContent,
                    :amendedBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("amendmentNo", amendmentNo)
                .addValue("amendmentReason", request.amendmentReason())
                .addValue("amendedContent", request.amendedContent())
                .addValue("amendedBy", userId);

        jdbc.update(insertSql, params);

        String updateSql = """
                UPDATE diagnostic_reports
                SET report_status = 'Amended',
                    updated_by = :amendedBy,
                    updated_at = NOW()
                WHERE id = :reportId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params);

        return new ReportAmendmentResponse(
                reportId,
                amendmentNo,
                request.amendmentReason(),
                request.amendedContent(),
                userId,
                LocalDateTime.now()
        );
    }

    public void insertAudit(Long tenantId,
                            Long hospitalId,
                            String branchId,
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
                INSERT INTO diagnostic_report_audit_events (
                    tenant_id,
                    hospital_id,
                    branch_id,
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

    public List<ReportAuditResponse> findAudit(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long reportId) {

        String sql = """
                SELECT *
                FROM diagnostic_report_audit_events
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND report_id = :reportId
                ORDER BY created_at DESC
                """;

        return jdbc.query(sql, baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> new ReportAuditResponse(
                        rs.getLong("id"),
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
                ));
    }

    private DiagnosticReportResponse mapReport(Long reportId, java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DiagnosticReportResponse(
                rs.getLong("id"),
                rs.getString("report_no"),
                rs.getString("source_type"),
                rs.getLong("source_id"),
                rs.getObject("diagnostic_order_id") == null ? null : rs.getLong("diagnostic_order_id"),
                rs.getObject("diagnostic_order_line_id") == null ? null : rs.getLong("diagnostic_order_line_id"),
                rs.getLong("patient_id"),
                rs.getString("patient_uhid"),
                rs.getString("patient_name"),
                rs.getString("department"),
                rs.getString("modality"),
                rs.getObject("service_id") == null ? null : rs.getLong("service_id"),
                rs.getString("service_code"),
                rs.getString("service_name"),
                rs.getString("report_status"),
                rs.getString("clinical_summary"),
                rs.getString("impression"),
                rs.getString("recommendation"),
                rs.getInt("abnormal_flag") == 1,
                rs.getInt("critical_flag") == 1,
                rs.getInt("expert_review_required") == 1,
                rs.getString("expert_review_status"),
                rs.getTimestamp("signed_at") == null ? null : rs.getTimestamp("signed_at").toLocalDateTime(),
                rs.getObject("signed_by") == null ? null : rs.getLong("signed_by"),
                rs.getString("signed_by_name"),
                rs.getTimestamp("released_at") == null ? null : rs.getTimestamp("released_at").toLocalDateTime(),
                rs.getObject("released_by") == null ? null : rs.getLong("released_by"),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                findSections(
                        rs.getLong("tenant_id"),
                        rs.getLong("hospital_id"),
                        rs.getString("branch_id"),
                        reportId
                ),
                findObservations(
                        rs.getLong("tenant_id"),
                        rs.getLong("hospital_id"),
                        rs.getString("branch_id"),
                        reportId
                )
        );
    }

    private List<ReportSectionResponse> findSections(Long tenantId, Long hospitalId, String branchId, Long reportId) {
        String sql = """
                SELECT id, section_title, section_content, display_order
                FROM diagnostic_report_sections
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND report_id = :reportId
                  AND is_deleted = 0
                ORDER BY display_order ASC, id ASC
                """;

        return jdbc.query(sql, baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> new ReportSectionResponse(
                        rs.getLong("id"),
                        rs.getString("section_title"),
                        rs.getString("section_content"),
                        rs.getInt("display_order")
                ));
    }

    private List<ReportObservationResponse> findObservations(Long tenantId, Long hospitalId, String branchId, Long reportId) {
        String sql = """
                SELECT id, parameter_code, parameter_name, result_value, unit,
                       reference_range, abnormal_flag, critical_flag, display_order
                FROM diagnostic_report_observations
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND report_id = :reportId
                  AND is_deleted = 0
                ORDER BY display_order ASC, id ASC
                """;

        return jdbc.query(sql, baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> new ReportObservationResponse(
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

    private void appendFilters(StringBuilder sql,
                               String status,
                               String sourceType,
                               String department,
                               String search) {

        if (status != null && !status.isBlank()) {
            sql.append(" AND report_status = :status ");
        }

        if (sourceType != null && !sourceType.isBlank()) {
            sql.append(" AND source_type = :sourceType ");
        }

        if (department != null && !department.isBlank()) {
            sql.append(" AND department = :department ");
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        report_no LIKE :search
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
                                             String sourceType,
                                             String department,
                                             String search) {

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);

        if (status != null && !status.isBlank()) {
            params.addValue("status", status);
        }

        if (sourceType != null && !sourceType.isBlank()) {
            params.addValue("sourceType", sourceType);
        }

        if (department != null && !department.isBlank()) {
            params.addValue("department", department);
        }

        if (search != null && !search.isBlank()) {
            params.addValue("search", "%" + search.trim() + "%");
        }

        return params;
    }

    private String signatureHash(Long reportId, Long userId, String signerName) {
        try {
            String raw = reportId + "|" + userId + "|" + signerName + "|" + System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes()));
        } catch (Exception ex) {
            return "SIGN-" + System.currentTimeMillis();
        }
    }

    private MapSqlParameterSource baseParams(Long tenantId, Long hospitalId, String branchId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("branchId", branchId);
    }
}