package com.plasmit.diagnostic.integration.integration.repository;

import com.plasmit.diagnostic.integration.common.exception.ApiException;
import com.plasmit.diagnostic.integration.integration.dto.request.*;
import com.plasmit.diagnostic.integration.integration.dto.response.*;
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
public class DiagnosticIntegrationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DiagnosticIntegrationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<IntegrationNodeResponse> findNodes(Long tenantId,
                                                   Long hospitalId,
                                                   String branchId,
                                                   String integrationType,
                                                   String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_integration_nodes
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (integrationType != null && !integrationType.isBlank()) {
            sql.append(" AND integration_type = :integrationType ");
            params.addValue("integrationType", integrationType);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND node_status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY node_name ASC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapNode(rs));
    }

    public IntegrationNodeResponse findNodeById(Long tenantId,
                                                Long hospitalId,
                                                String branchId,
                                                Long nodeId) {

        String sql = """
                SELECT *
                FROM diagnostic_integration_nodes
                WHERE id = :nodeId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<IntegrationNodeResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("nodeId", nodeId),
                (rs, rowNum) -> mapNode(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Integration node not found.");
        }

        return rows.get(0);
    }

    public void updateHeartbeat(Long tenantId,
                                Long hospitalId,
                                String branchId,
                                Long nodeId,
                                Long userId) {

        findNodeById(tenantId, hospitalId, branchId, nodeId);

        String sql = """
                UPDATE diagnostic_integration_nodes
                SET last_heartbeat_at = NOW(),
                    node_status = 'Active',
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :nodeId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("nodeId", nodeId)
                .addValue("updatedBy", userId));
    }

    public Long createExchangeEvent(Long tenantId,
                                    Long hospitalId,
                                    String branchId,
                                    Long userId,
                                    CreateExchangeEventRequest request,
                                    String eventCode) {

        findNodeById(tenantId, hospitalId, branchId, request.nodeId());

        String sql = """
                INSERT INTO diagnostic_integration_exchange_events (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    node_id,
                    event_code,
                    event_type,
                    direction,
                    reference_type,
                    reference_id,
                    exchange_status,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :nodeId,
                    :eventCode,
                    :eventType,
                    :direction,
                    :referenceType,
                    :referenceId,
                    'Received',
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("nodeId", request.nodeId())
                .addValue("eventCode", eventCode)
                .addValue("eventType", request.eventType())
                .addValue("direction", request.direction())
                .addValue("referenceType", request.referenceType())
                .addValue("referenceId", request.referenceId())
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void createPayload(Long tenantId,
                              Long hospitalId,
                              String branchId,
                              Long eventId,
                              String payloadType,
                              String contentType,
                              String rawPayload,
                              String parsedPayload) {

        String payloadForHash = rawPayload != null ? rawPayload : parsedPayload;

        String sql = """
                INSERT INTO diagnostic_integration_payloads (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    exchange_event_id,
                    payload_type,
                    content_type,
                    raw_payload,
                    parsed_payload,
                    checksum
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :eventId,
                    :payloadType,
                    :contentType,
                    :rawPayload,
                    :parsedPayload,
                    :checksum
                )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("eventId", eventId)
                .addValue("payloadType", payloadType)
                .addValue("contentType", contentType == null || contentType.isBlank() ? "application/json" : contentType)
                .addValue("rawPayload", rawPayload)
                .addValue("parsedPayload", parsedPayload)
                .addValue("checksum", checksum(payloadForHash)));
    }

    public Long countEvents(Long tenantId,
                            Long hospitalId,
                            String branchId,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String status,
                            String eventType,
                            Long nodeId,
                            String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM diagnostic_integration_exchange_events
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = eventListParams(
                tenantId, hospitalId, branchId, fromDate, toDate, status, eventType, nodeId, search
        );

        appendEventFilters(sql, status, eventType, nodeId, search);

        Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0L : total;
    }

    public List<ExchangeEventResponse> findEvents(Long tenantId,
                                                  Long hospitalId,
                                                  String branchId,
                                                  LocalDate fromDate,
                                                  LocalDate toDate,
                                                  String status,
                                                  String eventType,
                                                  Long nodeId,
                                                  String search,
                                                  Integer page,
                                                  Integer limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM diagnostic_integration_exchange_events
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = eventListParams(
                tenantId, hospitalId, branchId, fromDate, toDate, status, eventType, nodeId, search
        );

        appendEventFilters(sql, status, eventType, nodeId, search);

        sql.append("""
                ORDER BY created_at DESC
                LIMIT :limit OFFSET :offset
                """);

        params.addValue("limit", limit);
        params.addValue("offset", (page - 1) * limit);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapEvent(rs));
    }

    public ExchangeEventResponse findEventById(Long tenantId,
                                               Long hospitalId,
                                               String branchId,
                                               Long eventId) {

        String sql = """
                SELECT *
                FROM diagnostic_integration_exchange_events
                WHERE id = :eventId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        List<ExchangeEventResponse> rows = jdbc.query(
                sql,
                baseParams(tenantId, hospitalId, branchId).addValue("eventId", eventId),
                (rs, rowNum) -> mapEvent(rs)
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Exchange event not found.");
        }

        return rows.get(0);
    }

    public void updateEventStatus(Long tenantId,
                                  Long hospitalId,
                                  String branchId,
                                  Long eventId,
                                  String status,
                                  String errorMessage,
                                  Long userId) {

        String sql = """
                UPDATE diagnostic_integration_exchange_events
                SET exchange_status = :status,
                    error_message = :errorMessage,
                    processed_at = CASE
                        WHEN :status IN ('Processed', 'Failed') THEN NOW()
                        ELSE processed_at
                    END,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :eventId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("eventId", eventId)
                .addValue("status", status)
                .addValue("errorMessage", errorMessage)
                .addValue("updatedBy", userId));
    }

    public RetryLogResponse createRetryLog(Long tenantId,
                                           Long hospitalId,
                                           String branchId,
                                           ExchangeEventResponse event,
                                           Long userId,
                                           RetryExchangeEventRequest request) {

        int nextRetry = event.retryCount() + 1;

        if (nextRetry > event.maxRetryCount()) {
            throw ApiException.workflow("Maximum retry count exceeded.");
        }

        String sql = """
                INSERT INTO diagnostic_integration_retry_logs (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    exchange_event_id,
                    retry_no,
                    retry_status,
                    retry_reason,
                    next_retry_at,
                    actor_user_id
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :eventId,
                    :retryNo,
                    'Scheduled',
                    :retryReason,
                    :nextRetryAt,
                    :actorUserId
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("eventId", event.eventId())
                .addValue("retryNo", nextRetry)
                .addValue("retryReason", request.retryReason())
                .addValue("nextRetryAt", request.nextRetryAt() == null ? null : Timestamp.valueOf(request.nextRetryAt()))
                .addValue("actorUserId", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        String updateSql = """
                UPDATE diagnostic_integration_exchange_events
                SET exchange_status = 'RetryScheduled',
                    retry_count = :retryCount,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :eventId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                """;

        jdbc.update(updateSql, baseParams(tenantId, hospitalId, branchId)
                .addValue("eventId", event.eventId())
                .addValue("retryCount", nextRetry)
                .addValue("updatedBy", userId));

        return new RetryLogResponse(
                Objects.requireNonNull(keyHolder.getKey()).longValue(),
                event.eventId(),
                nextRetry,
                "Scheduled",
                request.retryReason(),
                LocalDateTime.now(),
                request.nextRetryAt(),
                userId
        );
    }

    public List<RetryLogResponse> findRetryLogs(Long tenantId,
                                                Long hospitalId,
                                                String branchId,
                                                Long eventId) {

        String sql = """
                SELECT *
                FROM diagnostic_integration_retry_logs
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND exchange_event_id = :eventId
                ORDER BY attempted_at DESC
                """;

        return jdbc.query(sql, baseParams(tenantId, hospitalId, branchId).addValue("eventId", eventId),
                (rs, rowNum) -> new RetryLogResponse(
                        rs.getLong("id"),
                        rs.getLong("exchange_event_id"),
                        rs.getInt("retry_no"),
                        rs.getString("retry_status"),
                        rs.getString("retry_reason"),
                        rs.getTimestamp("attempted_at") == null ? null : rs.getTimestamp("attempted_at").toLocalDateTime(),
                        rs.getTimestamp("next_retry_at") == null ? null : rs.getTimestamp("next_retry_at").toLocalDateTime(),
                        rs.getObject("actor_user_id") == null ? null : rs.getLong("actor_user_id")
                ));
    }

    public void insertAudit(Long tenantId,
                            Long hospitalId,
                            String branchId,
                            String entityType,
                            Long entityId,
                            String eventType,
                            String fromStatus,
                            String toStatus,
                            String notes,
                            Long userId,
                            String role,
                            String requestId) {

        String sql = """
                INSERT INTO diagnostic_integration_audit_events (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    entity_type,
                    entity_id,
                    event_type,
                    from_status,
                    to_status,
                    notes,
                    actor_user_id,
                    actor_role,
                    request_id
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :entityType,
                    :entityId,
                    :eventType,
                    :fromStatus,
                    :toStatus,
                    :notes,
                    :actorUserId,
                    :actorRole,
                    :requestId
                )
                """;

        jdbc.update(sql, baseParams(tenantId, hospitalId, branchId)
                .addValue("entityType", entityType)
                .addValue("entityId", entityId)
                .addValue("eventType", eventType)
                .addValue("fromStatus", fromStatus)
                .addValue("toStatus", toStatus)
                .addValue("notes", notes)
                .addValue("actorUserId", userId)
                .addValue("actorRole", role)
                .addValue("requestId", requestId));
    }

    public List<IntegrationAuditResponse> findAudit(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    String entityType,
                                                    Long entityId) {

        String sql = """
                SELECT *
                FROM diagnostic_integration_audit_events
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND entity_type = :entityType
                  AND entity_id = :entityId
                ORDER BY created_at ASC
                """;

        return jdbc.query(sql, baseParams(tenantId, hospitalId, branchId)
                        .addValue("entityType", entityType)
                        .addValue("entityId", entityId),
                (rs, rowNum) -> new IntegrationAuditResponse(
                        rs.getLong("id"),
                        rs.getString("entity_type"),
                        rs.getLong("entity_id"),
                        rs.getString("event_type"),
                        rs.getString("from_status"),
                        rs.getString("to_status"),
                        rs.getString("notes"),
                        rs.getObject("actor_user_id") == null ? null : rs.getLong("actor_user_id"),
                        rs.getString("actor_role"),
                        rs.getString("request_id"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                ));
    }

    private IntegrationNodeResponse mapNode(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new IntegrationNodeResponse(
                rs.getLong("id"),
                rs.getString("node_code"),
                rs.getString("node_name"),
                rs.getString("integration_type"),
                rs.getString("direction"),
                rs.getString("endpoint_url"),
                rs.getString("protocol"),
                rs.getString("auth_type"),
                rs.getString("node_status"),
                rs.getTimestamp("last_heartbeat_at") == null ? null : rs.getTimestamp("last_heartbeat_at").toLocalDateTime(),
                rs.getInt("failure_count"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private ExchangeEventResponse mapEvent(java.sql.ResultSet rs) throws java.sql.SQLException {
        Long eventId = rs.getLong("id");

        return new ExchangeEventResponse(
                eventId,
                rs.getLong("node_id"),
                rs.getString("event_code"),
                rs.getString("event_type"),
                rs.getString("direction"),
                rs.getString("reference_type"),
                rs.getObject("reference_id") == null ? null : rs.getLong("reference_id"),
                rs.getString("exchange_status"),
                rs.getInt("retry_count"),
                rs.getInt("max_retry_count"),
                rs.getString("error_message"),
                rs.getTimestamp("received_at") == null ? null : rs.getTimestamp("received_at").toLocalDateTime(),
                rs.getTimestamp("processed_at") == null ? null : rs.getTimestamp("processed_at").toLocalDateTime(),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                findPayloads(
                        rs.getLong("tenant_id"),
                        rs.getLong("hospital_id"),
                        rs.getString("branch_id"),
                        eventId
                )
        );
    }

    private List<ExchangePayloadResponse> findPayloads(Long tenantId,
                                                       Long hospitalId,
                                                       String branchId,
                                                       Long eventId) {

        String sql = """
                SELECT *
                FROM diagnostic_integration_payloads
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND exchange_event_id = :eventId
                  AND is_deleted = 0
                ORDER BY id ASC
                """;

        return jdbc.query(sql, baseParams(tenantId, hospitalId, branchId).addValue("eventId", eventId),
                (rs, rowNum) -> new ExchangePayloadResponse(
                        rs.getLong("id"),
                        rs.getLong("exchange_event_id"),
                        rs.getString("payload_type"),
                        rs.getString("content_type"),
                        rs.getString("raw_payload"),
                        rs.getString("parsed_payload"),
                        rs.getString("checksum"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                ));
    }

    private void appendEventFilters(StringBuilder sql,
                                    String status,
                                    String eventType,
                                    Long nodeId,
                                    String search) {

        if (status != null && !status.isBlank()) {
            sql.append(" AND exchange_status = :status ");
        }

        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND event_type = :eventType ");
        }

        if (nodeId != null) {
            sql.append(" AND node_id = :nodeId ");
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        event_code LIKE :search
                        OR reference_type LIKE :search
                        OR error_message LIKE :search
                    )
                    """);
        }
    }

    private MapSqlParameterSource eventListParams(Long tenantId,
                                                  Long hospitalId,
                                                  String branchId,
                                                  LocalDate fromDate,
                                                  LocalDate toDate,
                                                  String status,
                                                  String eventType,
                                                  Long nodeId,
                                                  String search) {

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);

        if (status != null && !status.isBlank()) {
            params.addValue("status", status);
        }

        if (eventType != null && !eventType.isBlank()) {
            params.addValue("eventType", eventType);
        }

        if (nodeId != null) {
            params.addValue("nodeId", nodeId);
        }

        if (search != null && !search.isBlank()) {
            params.addValue("search", "%" + search.trim() + "%");
        }

        return params;
    }

    private String checksum(String value) {
        try {
            if (value == null) {
                return null;
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes()));
        } catch (Exception ex) {
            return null;
        }
    }

    private MapSqlParameterSource baseParams(Long tenantId, Long hospitalId, String branchId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("branchId", branchId);
    }
}