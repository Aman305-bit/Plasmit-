package com.plasmit.diagnostic.report.delivery.repository;

import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.delivery.dto.request.CreateReportAccessTokenRequest;
import com.plasmit.diagnostic.report.delivery.dto.request.SendReportDeliveryRequest;
import com.plasmit.diagnostic.report.delivery.dto.response.*;
import com.plasmit.diagnostic.report.reports.dto.response.DiagnosticReportResponse;
import com.plasmit.diagnostic.report.reports.repository.DiagnosticReportRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Repository
public class ReportDeliveryRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final DiagnosticReportRepository reportRepository;

    public ReportDeliveryRepository(NamedParameterJdbcTemplate jdbc,
                                    DiagnosticReportRepository reportRepository) {
        this.jdbc = jdbc;
        this.reportRepository = reportRepository;
    }

    public ReportDeliveryResponse createDelivery(Long tenantId,
                                                 Long hospitalId,
                                                 String branchId,
                                                 Long reportId,
                                                 Long userId,
                                                 SendReportDeliveryRequest request,
                                                 String deliveryStatus,
                                                 String deliveryReference,
                                                 String failureReason) {

        String sql = """
                INSERT INTO diagnostic_report_deliveries (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
                    delivery_channel,
                    recipient_type,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    delivery_status,
                    delivery_reference,
                    failure_reason,
                    sent_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportId,
                    :deliveryChannel,
                    :recipientType,
                    :recipientName,
                    :recipientMobile,
                    :recipientEmail,
                    :deliveryStatus,
                    :deliveryReference,
                    :failureReason,
                    :sentBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("deliveryChannel", request.deliveryChannel())
                .addValue("recipientType", request.recipientType())
                .addValue("recipientName", request.recipientName())
                .addValue("recipientMobile", request.recipientMobile())
                .addValue("recipientEmail", request.recipientEmail())
                .addValue("deliveryStatus", deliveryStatus)
                .addValue("deliveryReference", deliveryReference)
                .addValue("failureReason", failureReason)
                .addValue("sentBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        Long deliveryId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return findDeliveryById(tenantId, hospitalId, branchId, deliveryId);
    }

    public ReportDeliveryResponse findDeliveryById(Long tenantId,
                                                   Long hospitalId,
                                                   String branchId,
                                                   Long deliveryId) {

        String sql = """
                SELECT
                    id,
                    report_id,
                    delivery_channel,
                    recipient_type,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    delivery_status,
                    delivery_reference,
                    failure_reason,
                    sent_by,
                    sent_at
                FROM diagnostic_report_deliveries
                WHERE id = :deliveryId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<ReportDeliveryResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("deliveryId", deliveryId),
                (rs, rowNum) -> new ReportDeliveryResponse(
                        rs.getLong("id"),
                        rs.getLong("report_id"),
                        rs.getString("delivery_channel"),
                        rs.getString("recipient_type"),
                        rs.getString("recipient_name"),
                        rs.getString("recipient_mobile"),
                        rs.getString("recipient_email"),
                        rs.getString("delivery_status"),
                        rs.getString("delivery_reference"),
                        rs.getString("failure_reason"),
                        rs.getObject("sent_by") == null ? null : rs.getLong("sent_by"),
                        rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime()
                )
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Report delivery not found.");
        }

        return rows.get(0);
    }

    public List<ReportDeliveryResponse> findDeliveriesByReport(Long tenantId,
                                                               Long hospitalId,
                                                               String branchId,
                                                               Long reportId) {

        String sql = """
                SELECT
                    id,
                    report_id,
                    delivery_channel,
                    recipient_type,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    delivery_status,
                    delivery_reference,
                    failure_reason,
                    sent_by,
                    sent_at
                FROM diagnostic_report_deliveries
                WHERE report_id = :reportId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                ORDER BY sent_at DESC
                """;

        return jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> new ReportDeliveryResponse(
                        rs.getLong("id"),
                        rs.getLong("report_id"),
                        rs.getString("delivery_channel"),
                        rs.getString("recipient_type"),
                        rs.getString("recipient_name"),
                        rs.getString("recipient_mobile"),
                        rs.getString("recipient_email"),
                        rs.getString("delivery_status"),
                        rs.getString("delivery_reference"),
                        rs.getString("failure_reason"),
                        rs.getObject("sent_by") == null ? null : rs.getLong("sent_by"),
                        rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime()
                )
        );
    }

    public ReportAccessTokenResponse createAccessToken(Long tenantId,
                                                       Long hospitalId,
                                                       String branchId,
                                                       Long reportId,
                                                       Long userId,
                                                       CreateReportAccessTokenRequest request,
                                                       String accessToken,
                                                       String accessUrl) {

        String sql = """
                INSERT INTO diagnostic_report_access_tokens (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
                    access_token,
                    access_url,
                    recipient_type,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    expires_at,
                    status,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportId,
                    :accessToken,
                    :accessUrl,
                    :recipientType,
                    :recipientName,
                    :recipientMobile,
                    :recipientEmail,
                    :expiresAt,
                    'Active',
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("accessToken", accessToken)
                .addValue("accessUrl", accessUrl)
                .addValue("recipientType", request.recipientType())
                .addValue("recipientName", request.recipientName())
                .addValue("recipientMobile", request.recipientMobile())
                .addValue("recipientEmail", request.recipientEmail())
                .addValue("expiresAt", Timestamp.valueOf(request.expiresAt()))
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        Long tokenId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return findAccessTokenById(tenantId, hospitalId, branchId, tokenId);
    }

    public ReportAccessTokenResponse findAccessTokenById(Long tenantId,
                                                         Long hospitalId,
                                                         String branchId,
                                                         Long tokenId) {

        String sql = """
                SELECT
                    id,
                    report_id,
                    access_token,
                    access_url,
                    recipient_type,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    expires_at,
                    status,
                    revoked_at,
                    revoked_by,
                    revoke_reason,
                    created_by,
                    created_at
                FROM diagnostic_report_access_tokens
                WHERE id = :tokenId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<ReportAccessTokenResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("tokenId", tokenId),
                (rs, rowNum) -> mapToken(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Report access token not found.");
        }

        return rows.get(0);
    }

    public List<ReportAccessTokenResponse> findAccessTokensByReport(Long tenantId,
                                                                    Long hospitalId,
                                                                    String branchId,
                                                                    Long reportId) {

        String sql = """
                SELECT
                    id,
                    report_id,
                    access_token,
                    access_url,
                    recipient_type,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    expires_at,
                    status,
                    revoked_at,
                    revoked_by,
                    revoke_reason,
                    created_by,
                    created_at
                FROM diagnostic_report_access_tokens
                WHERE report_id = :reportId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                ORDER BY created_at DESC
                """;

        return jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> mapToken(rs)
        );
    }

    public ReportAccessTokenResponse findAccessTokenPublic(String accessToken) {
        String sql = """
                SELECT
                    id,
                    report_id,
                    access_token,
                    access_url,
                    recipient_type,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    expires_at,
                    status,
                    revoked_at,
                    revoked_by,
                    revoke_reason,
                    created_by,
                    created_at,
                    tenant_id,
                    hospital_id,
                    branch_id
                FROM diagnostic_report_access_tokens
                WHERE access_token = :accessToken
                  AND is_deleted = 0
                """;

        List<ReportAccessTokenResponse> rows = jdbc.query(
                sql,
                new MapSqlParameterSource().addValue("accessToken", accessToken),
                (rs, rowNum) -> mapToken(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Report access token not found.");
        }

        return rows.get(0);
    }

    public TokenContext findTokenContext(String accessToken) {
        String sql = """
                SELECT id, tenant_id, hospital_id, branch_id, report_id, status, expires_at
                FROM diagnostic_report_access_tokens
                WHERE access_token = :accessToken
                  AND is_deleted = 0
                """;

        List<TokenContext> rows = jdbc.query(
                sql,
                new MapSqlParameterSource().addValue("accessToken", accessToken),
                (rs, rowNum) -> new TokenContext(
                        rs.getLong("id"),
                        rs.getLong("tenant_id"),
                        rs.getLong("hospital_id"),
                        rs.getString("branch_id"),
                        rs.getLong("report_id"),
                        rs.getString("status"),
                        rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toLocalDateTime()
                )
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Report access token not found.");
        }

        return rows.get(0);
    }

    public ReportAccessTokenResponse revokeToken(Long tenantId,
                                                 Long hospitalId,
                                                 String branchId,
                                                 Long tokenId,
                                                 Long userId,
                                                 String reason) {

        ReportAccessTokenResponse existing = findAccessTokenById(tenantId, hospitalId, branchId, tokenId);

        if ("Revoked".equals(existing.status())) {
            throw ApiException.workflow("Access token is already revoked.");
        }

        if ("Expired".equals(existing.status())) {
            throw ApiException.workflow("Expired access token cannot be revoked.");
        }

        String sql = """
                UPDATE diagnostic_report_access_tokens
                SET status = 'Revoked',
                    revoked_at = NOW(),
                    revoked_by = :revokedBy,
                    revoke_reason = :reason,
                    updated_at = NOW()
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

        return findAccessTokenById(tenantId, hospitalId, branchId, tokenId);
    }

    public void expireOldTokens() {
        String sql = """
                UPDATE diagnostic_report_access_tokens
                SET status = 'Expired',
                    updated_at = NOW()
                WHERE status = 'Active'
                  AND expires_at < NOW()
                  AND is_deleted = 0
                """;

        jdbc.update(sql, new MapSqlParameterSource());
    }

    public void insertAccessLog(Long tenantId,
                                Long hospitalId,
                                String branchId,
                                Long reportId,
                                Long tokenId,
                                String accessType,
                                String accessedBy,
                                String ipAddress,
                                String userAgent,
                                String requestId) {

        String sql = """
                INSERT INTO diagnostic_report_access_logs (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    report_id,
                    access_token_id,
                    access_type,
                    accessed_by,
                    ip_address,
                    user_agent,
                    request_id
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :reportId,
                    :accessTokenId,
                    :accessType,
                    :accessedBy,
                    :ipAddress,
                    :userAgent,
                    :requestId
                )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("reportId", reportId)
                .addValue("accessTokenId", tokenId)
                .addValue("accessType", accessType)
                .addValue("accessedBy", accessedBy)
                .addValue("ipAddress", ipAddress)
                .addValue("userAgent", userAgent)
                .addValue("requestId", requestId));
    }

    public List<ReportAccessLogResponse> findAccessLogs(Long tenantId,
                                                        Long hospitalId,
                                                        String branchId,
                                                        Long reportId) {

        String sql = """
                SELECT
                    id,
                    report_id,
                    access_token_id,
                    access_type,
                    accessed_by,
                    ip_address,
                    user_agent,
                    request_id,
                    created_at
                FROM diagnostic_report_access_logs
                WHERE report_id = :reportId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                ORDER BY created_at DESC
                """;

        return jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("reportId", reportId),
                (rs, rowNum) -> new ReportAccessLogResponse(
                        rs.getLong("id"),
                        rs.getLong("report_id"),
                        rs.getObject("access_token_id") == null ? null : rs.getLong("access_token_id"),
                        rs.getString("access_type"),
                        rs.getString("accessed_by"),
                        rs.getString("ip_address"),
                        rs.getString("user_agent"),
                        rs.getString("request_id"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                )
        );
    }

    public DiagnosticReportResponse getReport(Long tenantId,
                                              Long hospitalId,
                                              String branchId,
                                              Long reportId) {
        return reportRepository.findReportById(tenantId, hospitalId, branchId, reportId);
    }

    private ReportAccessTokenResponse mapToken(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ReportAccessTokenResponse(
                rs.getLong("id"),
                rs.getLong("report_id"),
                rs.getString("access_token"),
                rs.getString("access_url"),
                rs.getString("recipient_type"),
                rs.getString("recipient_name"),
                rs.getString("recipient_mobile"),
                rs.getString("recipient_email"),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toLocalDateTime(),
                rs.getObject("revoked_by") == null ? null : rs.getLong("revoked_by"),
                rs.getString("revoke_reason"),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private MapSqlParameterSource baseParams(Long tenantId, Long hospitalId, String branchId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("branchId", branchId);
    }

    public record TokenContext(
            Long tokenId,
            Long tenantId,
            Long hospitalId,
            String branchId,
            Long reportId,
            String status,
            java.time.LocalDateTime expiresAt
    ) {
    }
}