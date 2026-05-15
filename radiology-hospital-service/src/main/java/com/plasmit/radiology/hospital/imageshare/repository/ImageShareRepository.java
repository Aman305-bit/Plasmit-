package com.plasmit.radiology.hospital.imageshare.repository;

import com.plasmit.radiology.hospital.common.exception.ApiException;
import com.plasmit.radiology.hospital.imageshare.dto.request.CreateImageShareRequest;
import com.plasmit.radiology.hospital.imageshare.dto.response.ImageShareAccessLogResponse;
import com.plasmit.radiology.hospital.imageshare.dto.response.ImageShareResponse;
import com.plasmit.radiology.hospital.radiology.dto.response.RadiologyStudyResponse;
import com.plasmit.radiology.hospital.radiology.repository.RadiologyStudyRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Repository
public class ImageShareRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final RadiologyStudyRepository radiologyStudyRepository;

    public ImageShareRepository(NamedParameterJdbcTemplate jdbc,
                                RadiologyStudyRepository radiologyStudyRepository) {
        this.jdbc = jdbc;
        this.radiologyStudyRepository = radiologyStudyRepository;
    }

    public Long countShares(Long tenantId,
                            Long hospitalId,
                            String branchId,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String status,
                            Long studyId,
                            String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM radiology_image_shares
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = listParams(
                tenantId, hospitalId, branchId, fromDate, toDate, status, studyId, search
        );

        appendFilters(sql, status, studyId, search);

        Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0L : total;
    }

    public List<ImageShareResponse> findShares(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               LocalDate fromDate,
                                               LocalDate toDate,
                                               String status,
                                               Long studyId,
                                               String search,
                                               Integer page,
                                               Integer limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    study_id,
                    share_code,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    share_purpose,
                    access_scope,
                    share_url,
                    expires_at,
                    status,
                    revoked_at,
                    revoked_by,
                    revoke_reason,
                    created_by,
                    created_at
                FROM radiology_image_shares
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = listParams(
                tenantId, hospitalId, branchId, fromDate, toDate, status, studyId, search
        );

        appendFilters(sql, status, studyId, search);

        sql.append("""
                ORDER BY created_at DESC
                LIMIT :limit OFFSET :offset
                """);

        params.addValue("limit", limit);
        params.addValue("offset", (page - 1) * limit);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapShare(rs));
    }

    public ImageShareResponse createShare(Long tenantId,
                                          Long hospitalId,
                                          String branchId,
                                          Long userId,
                                          CreateImageShareRequest request,
                                          String shareCode,
                                          String secureToken,
                                          String shareUrl) {

        RadiologyStudyResponse study = radiologyStudyRepository.findStudyById(
                tenantId,
                hospitalId,
                branchId,
                request.studyId()
        );

        if (study.viewerUrl() == null || study.viewerUrl().isBlank()) {
            throw ApiException.workflow("PACS match required before image sharing.");
        }

        String sql = """
                INSERT INTO radiology_image_shares (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    study_id,
                    share_code,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    share_purpose,
                    access_scope,
                    secure_token,
                    share_url,
                    expires_at,
                    status,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :studyId,
                    :shareCode,
                    :recipientName,
                    :recipientMobile,
                    :recipientEmail,
                    :sharePurpose,
                    :accessScope,
                    :secureToken,
                    :shareUrl,
                    :expiresAt,
                    'Active',
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("studyId", request.studyId())
                .addValue("shareCode", shareCode)
                .addValue("recipientName", request.recipientName())
                .addValue("recipientMobile", request.recipientMobile())
                .addValue("recipientEmail", request.recipientEmail())
                .addValue("sharePurpose", request.sharePurpose())
                .addValue("accessScope", request.accessScope())
                .addValue("secureToken", secureToken)
                .addValue("shareUrl", shareUrl)
                .addValue("expiresAt", Timestamp.valueOf(request.expiresAt()))
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();

        insertAccessLog(
                tenantId,
                hospitalId,
                branchId,
                id,
                request.studyId(),
                "Created",
                String.valueOf(userId),
                null,
                null,
                null
        );

        return findShareById(tenantId, hospitalId, branchId, id);
    }

    public ImageShareResponse findShareById(Long tenantId,
                                            Long hospitalId,
                                            String branchId,
                                            Long shareId) {

        String sql = """
                SELECT
                    id,
                    study_id,
                    share_code,
                    recipient_name,
                    recipient_mobile,
                    recipient_email,
                    share_purpose,
                    access_scope,
                    share_url,
                    expires_at,
                    status,
                    revoked_at,
                    revoked_by,
                    revoke_reason,
                    created_by,
                    created_at
                FROM radiology_image_shares
                WHERE id = :shareId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<ImageShareResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("shareId", shareId),
                (rs, rowNum) -> mapShare(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Image share not found.");
        }

        return rows.get(0);
    }

    public ImageShareResponse revokeShare(Long tenantId,
                                          Long hospitalId,
                                          String branchId,
                                          Long shareId,
                                          Long userId,
                                          String reason) {

        ImageShareResponse existing = findShareById(tenantId, hospitalId, branchId, shareId);

        if ("Revoked".equals(existing.status())) {
            throw ApiException.workflow("Image share is already revoked.");
        }

        if ("Expired".equals(existing.status())) {
            throw ApiException.workflow("Expired image share cannot be revoked.");
        }

        String sql = """
                UPDATE radiology_image_shares
                SET status = 'Revoked',
                    revoked_at = NOW(),
                    revoked_by = :revokedBy,
                    revoke_reason = :reason,
                    updated_at = NOW()
                WHERE id = :shareId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("shareId", shareId)
                .addValue("revokedBy", userId)
                .addValue("reason", reason);

        int updated = jdbc.update(sql, params);

        if (updated == 0) {
            throw ApiException.notFound("Image share not found.");
        }

        insertAccessLog(
                tenantId,
                hospitalId,
                branchId,
                shareId,
                existing.studyId(),
                "Revoked",
                String.valueOf(userId),
                null,
                null,
                null
        );

        return findShareById(tenantId, hospitalId, branchId, shareId);
    }

    public List<ImageShareAccessLogResponse> findAccessLogs(Long tenantId,
                                                            Long hospitalId,
                                                            String branchId,
                                                            Long shareId) {

        findShareById(tenantId, hospitalId, branchId, shareId);

        String sql = """
                SELECT
                    id,
                    share_id,
                    study_id,
                    access_type,
                    accessed_by,
                    ip_address,
                    user_agent,
                    request_id,
                    created_at
                FROM radiology_image_share_access_logs
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND share_id = :shareId
                ORDER BY created_at DESC
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("shareId", shareId);

        return jdbc.query(sql, params, (rs, rowNum) -> new ImageShareAccessLogResponse(
                rs.getLong("id"),
                rs.getLong("share_id"),
                rs.getLong("study_id"),
                rs.getString("access_type"),
                rs.getString("accessed_by"),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getString("request_id"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    public void expireOldShares() {
        String sql = """
                UPDATE radiology_image_shares
                SET status = 'Expired',
                    updated_at = NOW()
                WHERE status = 'Active'
                  AND expires_at < NOW()
                  AND is_deleted = 0
                """;

        jdbc.update(sql, new MapSqlParameterSource());
    }

    private void insertAccessLog(Long tenantId,
                                 Long hospitalId,
                                 String branchId,
                                 Long shareId,
                                 Long studyId,
                                 String accessType,
                                 String accessedBy,
                                 String ipAddress,
                                 String userAgent,
                                 String requestId) {

        String sql = """
                INSERT INTO radiology_image_share_access_logs (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    share_id,
                    study_id,
                    access_type,
                    accessed_by,
                    ip_address,
                    user_agent,
                    request_id
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :shareId,
                    :studyId,
                    :accessType,
                    :accessedBy,
                    :ipAddress,
                    :userAgent,
                    :requestId
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("shareId", shareId)
                .addValue("studyId", studyId)
                .addValue("accessType", accessType)
                .addValue("accessedBy", accessedBy)
                .addValue("ipAddress", ipAddress)
                .addValue("userAgent", userAgent)
                .addValue("requestId", requestId);

        jdbc.update(sql, params);
    }

    private ImageShareResponse mapShare(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ImageShareResponse(
                rs.getLong("id"),
                rs.getLong("study_id"),
                rs.getString("share_code"),
                rs.getString("recipient_name"),
                rs.getString("recipient_mobile"),
                rs.getString("recipient_email"),
                rs.getString("share_purpose"),
                rs.getString("access_scope"),
                rs.getString("share_url"),
                rs.getTimestamp("expires_at") == null ? null : rs.getTimestamp("expires_at").toLocalDateTime(),
                rs.getString("status"),
                rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toLocalDateTime(),
                rs.getObject("revoked_by") == null ? null : rs.getLong("revoked_by"),
                rs.getString("revoke_reason"),
                rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private void appendFilters(StringBuilder sql,
                               String status,
                               Long studyId,
                               String search) {

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status ");
        }

        if (studyId != null) {
            sql.append(" AND study_id = :studyId ");
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        share_code LIKE :search
                        OR recipient_name LIKE :search
                        OR recipient_mobile LIKE :search
                        OR recipient_email LIKE :search
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
                                             Long studyId,
                                             String search) {

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);

        if (status != null && !status.isBlank()) {
            params.addValue("status", status);
        }

        if (studyId != null) {
            params.addValue("studyId", studyId);
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