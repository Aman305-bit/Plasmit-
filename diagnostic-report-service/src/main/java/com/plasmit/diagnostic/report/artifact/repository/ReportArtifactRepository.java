package com.plasmit.diagnostic.report.artifact.repository;

import com.plasmit.diagnostic.report.artifact.dto.request.*;
import com.plasmit.diagnostic.report.artifact.dto.response.*;
import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.reports.dto.response.DiagnosticReportResponse;
import com.plasmit.diagnostic.report.reports.repository.DiagnosticReportRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Repository
public class ReportArtifactRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DiagnosticReportRepository reportRepository;

    public ReportArtifactRepository(NamedParameterJdbcTemplate jdbc,
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

    public ReportTemplateResponse createTemplate(Long tenantId,
                                                 Long hospitalId,
                                                 String branchId,
                                                 Long userId,
                                                 CreateReportTemplateRequest request) {
        String sql = """
                INSERT INTO diagnostic_report_templates (
                    tenant_id, hospital_id, branch_id,
                    template_code, template_name, department, modality,
                    template_body, header_html, footer_html,
                    created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :templateCode, :templateName, :department, :modality,
                    :templateBody, :headerHtml, :footerHtml,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("templateCode", request.templateCode())
                .addValue("templateName", request.templateName())
                .addValue("department", request.department())
                .addValue("modality", request.modality())
                .addValue("templateBody", request.templateBody())
                .addValue("headerHtml", request.headerHtml())
                .addValue("footerHtml", request.footerHtml())
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return findTemplateById(tenantId, hospitalId, branchId, Objects.requireNonNull(keyHolder.getKey()).longValue());
    }

    public List<ReportTemplateResponse> findTemplates(Long tenantId,
                                                      Long hospitalId,
                                                      String branchId,
                                                      String department,
                                                      String modality,
                                                      String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_report_templates
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (department != null && !department.isBlank()) {
            sql.append(" AND department = :department ");
            params.addValue("department", department);
        }

        if (modality != null && !modality.isBlank()) {
            sql.append(" AND modality = :modality ");
            params.addValue("modality", modality);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY template_name ASC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapTemplate(rs));
    }

    public ReportTemplateResponse findTemplateById(Long tenantId,
                                                   Long hospitalId,
                                                   String branchId,
                                                   Long templateId) {
        String sql = """
                SELECT *
                FROM diagnostic_report_templates
                WHERE id = :templateId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<ReportTemplateResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("templateId", templateId),
                (rs, rowNum) -> mapTemplate(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Report template not found.");
        }

        return rows.get(0);
    }

    public Integer nextVersionNo(Long tenantId,
                                 Long hospitalId,
                                 String branchId,
                                 Long reportId) {
        String sql = """
                SELECT COALESCE(MAX(version_no), 0) + 1
                FROM diagnostic_report_versions
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND report_id = :reportId
                """;

        Integer versionNo = jdbc.queryForObject(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                Integer.class
        );

        return versionNo == null ? 1 : versionNo;
    }

    public ReportVersionResponse createVersion(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long userId,
                                               Long reportId,
                                               String snapshotJson,
                                               CreateReportVersionRequest request) {
        Integer versionNo = nextVersionNo(tenantId, hospitalId, branchId, reportId);

        String sql = """
                INSERT INTO diagnostic_report_versions (
                    tenant_id, hospital_id, branch_id,
                    report_id, version_no, version_status,
                    version_reason, snapshot_json, created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :reportId, :versionNo, 'Created',
                    :versionReason, :snapshotJson, :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("versionNo", versionNo)
                .addValue("versionReason", request == null ? null : request.versionReason())
                .addValue("snapshotJson", snapshotJson)
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return findVersionById(tenantId, hospitalId, branchId, Objects.requireNonNull(keyHolder.getKey()).longValue());
    }

    public List<ReportVersionResponse> findVersions(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    Long reportId) {
        String sql = """
                SELECT *
                FROM diagnostic_report_versions
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND report_id = :reportId
                ORDER BY version_no DESC
                """;

        return jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> mapVersion(rs)
        );
    }

    public ReportVersionResponse findVersionById(Long tenantId,
                                                 Long hospitalId,
                                                 String branchId,
                                                 Long versionId) {
        String sql = """
                SELECT *
                FROM diagnostic_report_versions
                WHERE id = :versionId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                """;

        List<ReportVersionResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("versionId", versionId),
                (rs, rowNum) -> mapVersion(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Report version not found.");
        }

        return rows.get(0);
    }

    public PdfArtifactResponse createPdfArtifact(Long tenantId,
                                                 Long hospitalId,
                                                 String branchId,
                                                 Long userId,
                                                 Long reportId,
                                                 GeneratePdfArtifactRequest request,
                                                 String artifactNo,
                                                 String fileName,
                                                 String contentHash) {
        String sql = """
                INSERT INTO diagnostic_report_pdf_artifacts (
                    tenant_id, hospital_id, branch_id,
                    report_id, version_id, artifact_no, artifact_type,
                    file_name, file_path, file_url,
                    content_hash, generation_status, generated_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :reportId, :versionId, :artifactNo, 'PDF',
                    :fileName, :filePath, :fileUrl,
                    :contentHash, 'Generated', :generatedBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("versionId", request == null ? null : request.versionId())
                .addValue("artifactNo", artifactNo)
                .addValue("fileName", fileName)
                .addValue("filePath", request == null ? null : request.filePath())
                .addValue("fileUrl", request == null ? null : request.fileUrl())
                .addValue("contentHash", contentHash)
                .addValue("generatedBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return findArtifactById(tenantId, hospitalId, branchId, Objects.requireNonNull(keyHolder.getKey()).longValue());
    }

    public List<PdfArtifactResponse> findArtifacts(Long tenantId,
                                                   Long hospitalId,
                                                   String branchId,
                                                   Long reportId) {
        String sql = """
                SELECT *
                FROM diagnostic_report_pdf_artifacts
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND report_id = :reportId
                  AND is_deleted = 0
                ORDER BY generated_at DESC
                """;

        return jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> mapArtifact(rs)
        );
    }

    public PdfArtifactResponse findArtifactById(Long tenantId,
                                                Long hospitalId,
                                                String branchId,
                                                Long artifactId) {
        String sql = """
                SELECT *
                FROM diagnostic_report_pdf_artifacts
                WHERE id = :artifactId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<PdfArtifactResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("artifactId", artifactId),
                (rs, rowNum) -> mapArtifact(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("PDF artifact not found.");
        }

        return rows.get(0);
    }

    public QrTokenResponse createQrToken(Long tenantId,
                                         Long hospitalId,
                                         String branchId,
                                         Long userId,
                                         Long reportId,
                                         CreateQrTokenRequest request,
                                         String qrToken,
                                         String verificationUrl) {
        String sql = """
                INSERT INTO diagnostic_report_qr_tokens (
                    tenant_id, hospital_id, branch_id,
                    report_id, artifact_id,
                    qr_token, verification_url,
                    status, expires_at, created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :reportId, :artifactId,
                    :qrToken, :verificationUrl,
                    'Active', :expiresAt, :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("artifactId", request == null ? null : request.artifactId())
                .addValue("qrToken", qrToken)
                .addValue("verificationUrl", verificationUrl)
                .addValue("expiresAt", request == null || request.expiresAt() == null ? null : Timestamp.valueOf(request.expiresAt()))
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return findQrTokenById(tenantId, hospitalId, branchId, Objects.requireNonNull(keyHolder.getKey()).longValue());
    }

    public QrTokenResponse findQrTokenById(Long tenantId,
                                           Long hospitalId,
                                           String branchId,
                                           Long tokenId) {
        String sql = """
                SELECT *
                FROM diagnostic_report_qr_tokens
                WHERE id = :tokenId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<QrTokenResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("tokenId", tokenId),
                (rs, rowNum) -> mapQrToken(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("QR token not found.");
        }

        return rows.get(0);
    }

    public QrTokenContext findQrTokenContext(String qrToken) {
        String sql = """
                SELECT *
                FROM diagnostic_report_qr_tokens
                WHERE qr_token = :qrToken
                  AND is_deleted = 0
                """;

        List<QrTokenContext> rows = jdbc.query(
                sql,
                new MapSqlParameterSource().addValue("qrToken", qrToken),
                (rs, rowNum) -> new QrTokenContext(
                        rs.getLong("id"),
                        rs.getLong("tenant_id"),
                        rs.getLong("hospital_id"),
                        rs.getString("branch_id"),
                        rs.getLong("report_id"),
                        rs.getObject("artifact_id") == null ? null : rs.getLong("artifact_id"),
                        rs.getString("status"),
                        rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toLocalDateTime()
                )
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("QR token not found.");
        }

        return rows.get(0);
    }

    public QrTokenResponse revokeQrToken(Long tenantId,
                                         Long hospitalId,
                                         String branchId,
                                         Long userId,
                                         Long tokenId,
                                         String reason) {
        QrTokenResponse token = findQrTokenById(tenantId, hospitalId, branchId, tokenId);

        if ("Revoked".equals(token.status())) {
            throw ApiException.workflow("QR token is already revoked.");
        }

        String sql = """
                UPDATE diagnostic_report_qr_tokens
                SET status = 'Revoked',
                    revoked_at = NOW(),
                    revoked_by = :revokedBy,
                    revoke_reason = :reason
                WHERE id = :tokenId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("tokenId", tokenId)
                .addValue("revokedBy", userId)
                .addValue("reason", reason));

        return findQrTokenById(tenantId, hospitalId, branchId, tokenId);
    }

    public void insertVerificationLog(Long tenantId,
                                      Long hospitalId,
                                      String branchId,
                                      Long reportId,
                                      Long qrTokenId,
                                      String verificationStatus,
                                      String verifiedBy,
                                      String ipAddress,
                                      String userAgent,
                                      String requestId) {
        String sql = """
                INSERT INTO diagnostic_report_verification_logs (
                    tenant_id, hospital_id, branch_id,
                    report_id, qr_token_id,
                    verification_status, verified_by,
                    ip_address, user_agent, request_id
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :reportId, :qrTokenId,
                    :verificationStatus, :verifiedBy,
                    :ipAddress, :userAgent, :requestId
                )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("qrTokenId", qrTokenId)
                .addValue("verificationStatus", verificationStatus)
                .addValue("verifiedBy", verifiedBy)
                .addValue("ipAddress", ipAddress)
                .addValue("userAgent", userAgent)
                .addValue("requestId", requestId));
    }

    public List<VerificationLogResponse> findVerificationLogs(Long tenantId,
                                                              Long hospitalId,
                                                              String branchId,
                                                              Long reportId) {
        String sql = """
                SELECT *
                FROM diagnostic_report_verification_logs
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND report_id = :reportId
                ORDER BY created_at DESC
                """;

        return jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> new VerificationLogResponse(
                        rs.getLong("id"),
                        rs.getLong("report_id"),
                        rs.getObject("qr_token_id") == null ? null : rs.getLong("qr_token_id"),
                        rs.getString("verification_status"),
                        rs.getString("verified_by"),
                        rs.getString("ip_address"),
                        rs.getString("user_agent"),
                        rs.getString("request_id"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                )
        );
    }

    public PdfArtifactResponse findLatestArtifactNullable(Long tenantId,
                                                          Long hospitalId,
                                                          String branchId,
                                                          Long reportId) {
        String sql = """
                SELECT *
                FROM diagnostic_report_pdf_artifacts
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND report_id = :reportId
                  AND is_deleted = 0
                ORDER BY generated_at DESC
                LIMIT 1
                """;

        List<PdfArtifactResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> mapArtifact(rs)
        );

        return rows.isEmpty() ? null : rows.get(0);
    }

    private ReportTemplateResponse mapTemplate(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ReportTemplateResponse(
                rs.getLong("id"),
                rs.getString("template_code"),
                rs.getString("template_name"),
                rs.getString("department"),
                rs.getString("modality"),
                rs.getString("template_body"),
                rs.getString("header_html"),
                rs.getString("footer_html"),
                rs.getString("status"),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private ReportVersionResponse mapVersion(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ReportVersionResponse(
                rs.getLong("id"),
                rs.getLong("report_id"),
                rs.getInt("version_no"),
                rs.getString("version_status"),
                rs.getString("version_reason"),
                rs.getString("snapshot_json"),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private PdfArtifactResponse mapArtifact(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PdfArtifactResponse(
                rs.getLong("id"),
                rs.getLong("report_id"),
                rs.getObject("version_id") == null ? null : rs.getLong("version_id"),
                rs.getString("artifact_no"),
                rs.getString("artifact_type"),
                rs.getString("file_name"),
                rs.getString("file_path"),
                rs.getString("file_url"),
                rs.getString("content_hash"),
                rs.getString("generation_status"),
                rs.getObject("generated_by") == null ? null : rs.getLong("generated_by"),
                rs.getTimestamp("generated_at") == null ? null : rs.getTimestamp("generated_at").toLocalDateTime()
        );
    }

    private QrTokenResponse mapQrToken(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new QrTokenResponse(
                rs.getLong("id"),
                rs.getLong("report_id"),
                rs.getObject("artifact_id") == null ? null : rs.getLong("artifact_id"),
                rs.getString("qr_token"),
                rs.getString("verification_url"),
                rs.getString("status"),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toLocalDateTime(),
                rs.getObject("revoked_by") == null ? null : rs.getLong("revoked_by"),
                rs.getString("revoke_reason")
        );
    }

    public String hash(String value) {
        try {
            if (value == null) {
                return null;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes()));
        } catch (Exception ex) {
            return "HASH-" + System.currentTimeMillis();
        }
    }

    private MapSqlParameterSource baseParams(Long tenantId, Long hospitalId, String branchId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("branchId", branchId);
    }

    public record QrTokenContext(
            Long tokenId,
            Long tenantId,
            Long hospitalId,
            String branchId,
            Long reportId,
            Long artifactId,
            String status,
            LocalDateTime expiresAt
    ) {
    }
}