package com.plasmit.diagnostics.payment.diagnostics.repository;

import com.plasmit.diagnostics.payment.common.exception.ApiException;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.BillingAuthorizationRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.request.DiagnosticOrderCreateRequest;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.BillingAuthorizationResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.DiagnosticOrderLineResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.DiagnosticOrderResponse;
import com.plasmit.diagnostics.payment.diagnostics.dto.response.DiagnosticsSummaryResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class DiagnosticOrderRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DiagnosticOrderRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> findBranches(Long tenantId, Long hospitalId) {
        String sql = """
                SELECT DISTINCT branch_id
                FROM diagnostic_orders
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND is_deleted = 0

                UNION

                SELECT DISTINCT branch_id
                FROM diagnostic_services
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND is_deleted = 0
                ORDER BY branch_id
                """;

        return jdbc.queryForList(sql, tenantHospitalParams(tenantId, hospitalId), String.class);
    }

    public List<String> findDepartments(Long tenantId, Long hospitalId, String branchId) {
        String sql = """
                SELECT DISTINCT department
                FROM diagnostic_services
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                  AND is_active = 1
                ORDER BY department
                """;

        return jdbc.queryForList(sql, baseParams(tenantId, hospitalId, branchId), String.class);
    }

    public List<String> findModalities(Long tenantId, Long hospitalId, String branchId) {
        String sql = """
                SELECT DISTINCT modality
                FROM diagnostic_services
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                  AND is_active = 1
                ORDER BY modality
                """;

        return jdbc.queryForList(sql, baseParams(tenantId, hospitalId, branchId), String.class);
    }

    public DiagnosticsSummaryResponse getSummary(Long tenantId,
                                                 Long hospitalId,
                                                 String branchId) {

        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN status NOT IN ('Released','Delivered','Cancelled','Rejected') THEN 1 ELSE 0 END), 0) AS open_orders,
                    COALESCE(SUM(CASE WHEN department = 'Radiology' THEN 1 ELSE 0 END), 0) AS radiology_studies,
                    COALESCE(SUM(CASE WHEN department = 'Pathology' THEN 1 ELSE 0 END), 0) AS pathology_specimens,
                    COALESCE(SUM(CASE WHEN due_at IS NOT NULL
                                        AND due_at < NOW()
                                        AND status NOT IN ('Released','Delivered','Cancelled','Rejected')
                                      THEN 1 ELSE 0 END), 0) AS tat_breaches,
                    COALESCE(SUM(CASE WHEN priority = 'STAT'
                                        AND status NOT IN ('Released','Delivered','Cancelled','Rejected')
                                      THEN 1 ELSE 0 END), 0) AS critical_alerts_pending,
                    COALESCE(SUM(CASE WHEN status IN ('TechnicalVerified','ClinicalVerified','Signed') THEN 1 ELSE 0 END), 0) AS reports_pending_signoff,
                    (
                        SELECT COUNT(*)
                        FROM diagnostic_invoice_handoffs h
                        WHERE h.tenant_id = :tenantId
                          AND h.hospital_id = :hospitalId
                          AND h.branch_id = :branchId
                          AND h.is_deleted = 0
                          AND h.handoff_status IN ('Pending','Failed')
                    ) AS integration_queue,
                    COALESCE(SUM(CASE WHEN status = 'Rejected' THEN 1 ELSE 0 END), 0) AS quality_blocked
                FROM diagnostic_orders
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        return jdbc.queryForObject(sql, baseParams(tenantId, hospitalId, branchId), (rs, rowNum) ->
                new DiagnosticsSummaryResponse(
                        rs.getLong("open_orders"),
                        rs.getLong("radiology_studies"),
                        rs.getLong("pathology_specimens"),
                        rs.getLong("tat_breaches"),
                        rs.getLong("critical_alerts_pending"),
                        rs.getLong("reports_pending_signoff"),
                        rs.getLong("integration_queue"),
                        rs.getLong("quality_blocked")
                )
        );
    }

    public Long countOrders(Long tenantId,
                            Long hospitalId,
                            String branchId,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String department,
                            String status,
                            String priority,
                            String source,
                            String search) {

        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM diagnostic_orders
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = orderFilterParams(
                tenantId, hospitalId, branchId, fromDate, toDate,
                department, status, priority, source, search
        );

        appendOrderFilters(sql, department, status, priority, source, search);

        Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0L : total;
    }

    public List<DiagnosticOrderResponse> findOrders(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String department,
                                                    String status,
                                                    String priority,
                                                    String source,
                                                    String search,
                                                    Integer page,
                                                    Integer limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    order_no,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    encounter_id,
                    encounter_type,
                    source,
                    department,
                    priority,
                    status,
                    clinical_notes,
                    diagnosis_codes_json,
                    payer_context_id,
                    preferred_at,
                    due_at,
                    flags_json,
                    accession_no,
                    barcode_no,
                    billing_authorization_status,
                    billing_release_blocked,
                    invoice_id,
                    created_by,
                    created_at
                FROM diagnostic_orders
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(created_at) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """);

        MapSqlParameterSource params = orderFilterParams(
                tenantId, hospitalId, branchId, fromDate, toDate,
                department, status, priority, source, search
        );

        appendOrderFilters(sql, department, status, priority, source, search);

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

        List<DiagnosticOrderResponse> orders = jdbc.query(sql.toString(), params, (rs, rowNum) ->
                mapOrder(rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getLong("patient_id"),
                        rs.getString("patient_uhid"),
                        rs.getString("patient_name"),
                        rs.getObject("encounter_id") == null ? null : rs.getLong("encounter_id"),
                        rs.getString("encounter_type"),
                        rs.getString("source"),
                        rs.getString("department"),
                        rs.getString("priority"),
                        rs.getString("status"),
                        rs.getString("clinical_notes"),
                        parseJsonArray(rs.getString("diagnosis_codes_json")),
                        rs.getString("payer_context_id"),
                        rs.getTimestamp("preferred_at") == null ? null : rs.getTimestamp("preferred_at").toLocalDateTime(),
                        rs.getTimestamp("due_at") == null ? null : rs.getTimestamp("due_at").toLocalDateTime(),
                        parseJsonArray(rs.getString("flags_json")),
                        rs.getString("accession_no"),
                        rs.getString("barcode_no"),
                        rs.getString("billing_authorization_status"),
                        rs.getInt("billing_release_blocked") == 1,
                        rs.getObject("invoice_id") == null ? null : rs.getLong("invoice_id"),
                        Collections.emptyList(),
                        rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
                )
        );

        for (int i = 0; i < orders.size(); i++) {
            DiagnosticOrderResponse order = orders.get(i);
            orders.set(i, withLines(order, findOrderLines(tenantId, hospitalId, branchId, order.orderId())));
        }

        return orders;
    }

    public Long countDuplicatePending(Long tenantId,
                                      Long hospitalId,
                                      String branchId,
                                      Long patientId,
                                      List<Long> serviceIds) {

        String sql = """
                SELECT COUNT(*)
                FROM diagnostic_orders o
                INNER JOIN diagnostic_order_lines l ON l.order_id = o.id
                WHERE o.tenant_id = :tenantId
                  AND o.hospital_id = :hospitalId
                  AND o.branch_id = :branchId
                  AND o.patient_id = :patientId
                  AND l.service_id IN (:serviceIds)
                  AND o.status NOT IN ('Released','Delivered','Cancelled','Rejected')
                  AND o.is_deleted = 0
                  AND l.is_deleted = 0
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("patientId", patientId)
                .addValue("serviceIds", serviceIds);

        Long total = jdbc.queryForObject(sql, params, Long.class);
        return total == null ? 0L : total;
    }

    public List<Map<String, Object>> findServicesForOrder(Long tenantId,
                                                          Long hospitalId,
                                                          String branchId,
                                                          List<Long> serviceIds) {

        String sql = """
                SELECT
                    id,
                    service_code,
                    service_name,
                    department,
                    modality
                FROM diagnostic_services
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND id IN (:serviceIds)
                  AND is_deleted = 0
                  AND is_active = 1
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("serviceIds", serviceIds);

        return jdbc.queryForList(sql, params);
    }

    public Long createOrder(Long tenantId,
                            Long hospitalId,
                            String branchId,
                            Long userId,
                            DiagnosticOrderCreateRequest request,
                            String orderNo,
                            String diagnosisCodesJson,
                            String flagsJson,
                            String accessionNo,
                            String barcodeNo,
                            LocalDateTime dueAt,
                            String billingAuthStatus,
                            boolean releaseBlocked) {

        String sql = """
                INSERT INTO diagnostic_orders (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    order_no,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    encounter_id,
                    encounter_type,
                    source,
                    department,
                    priority,
                    status,
                    clinical_notes,
                    diagnosis_codes_json,
                    payer_context_id,
                    preferred_at,
                    due_at,
                    flags_json,
                    accession_no,
                    barcode_no,
                    billing_authorization_status,
                    billing_release_blocked,
                    invoice_id,
                    created_by
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :orderNo,
                    :patientId,
                    :patientUhid,
                    :patientName,
                    :encounterId,
                    :encounterType,
                    :source,
                    :department,
                    :priority,
                    :status,
                    :clinicalNotes,
                    CAST(:diagnosisCodesJson AS JSON),
                    :payerContextId,
                    :preferredAt,
                    :dueAt,
                    CAST(:flagsJson AS JSON),
                    :accessionNo,
                    :barcodeNo,
                    :billingAuthStatus,
                    :releaseBlocked,
                    :invoiceId,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("orderNo", orderNo)
                .addValue("patientId", request.patientId())
                .addValue("patientUhid", request.patientUhid())
                .addValue("patientName", request.patientName())
                .addValue("encounterId", request.encounterId())
                .addValue("encounterType", request.encounterType())
                .addValue("source", request.source())
                .addValue("department", request.department())
                .addValue("priority", request.priority())
                .addValue("status", "Ordered")
                .addValue("clinicalNotes", request.clinicalNotes())
                .addValue("diagnosisCodesJson", diagnosisCodesJson)
                .addValue("payerContextId", request.payerContextId())
                .addValue("preferredAt", request.preferredAt() == null ? null : Timestamp.valueOf(request.preferredAt()))
                .addValue("dueAt", dueAt == null ? null : Timestamp.valueOf(dueAt))
                .addValue("flagsJson", flagsJson)
                .addValue("accessionNo", accessionNo)
                .addValue("barcodeNo", barcodeNo)
                .addValue("billingAuthStatus", billingAuthStatus)
                .addValue("releaseBlocked", releaseBlocked ? 1 : 0)
                .addValue("invoiceId", request.invoiceId())
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public Long createOrderLine(Long tenantId,
                                Long hospitalId,
                                String branchId,
                                Long orderId,
                                Map<String, Object> service) {

        String sql = """
                INSERT INTO diagnostic_order_lines (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    order_id,
                    service_id,
                    service_code,
                    service_name,
                    department,
                    modality,
                    status
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :orderId,
                    :serviceId,
                    :serviceCode,
                    :serviceName,
                    :department,
                    :modality,
                    'Ordered'
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("orderId", orderId)
                .addValue("serviceId", service.get("id"))
                .addValue("serviceCode", service.get("service_code"))
                .addValue("serviceName", service.get("service_name"))
                .addValue("department", service.get("department"))
                .addValue("modality", service.get("modality"));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public DiagnosticOrderResponse findOrderById(Long tenantId,
                                                 Long hospitalId,
                                                 String branchId,
                                                 Long orderId) {

        String sql = """
                SELECT
                    id,
                    order_no,
                    patient_id,
                    patient_uhid,
                    patient_name,
                    encounter_id,
                    encounter_type,
                    source,
                    department,
                    priority,
                    status,
                    clinical_notes,
                    diagnosis_codes_json,
                    payer_context_id,
                    preferred_at,
                    due_at,
                    flags_json,
                    accession_no,
                    barcode_no,
                    billing_authorization_status,
                    billing_release_blocked,
                    invoice_id,
                    created_by,
                    created_at
                FROM diagnostic_orders
                WHERE id = :orderId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("orderId", orderId);

        List<DiagnosticOrderResponse> rows = jdbc.query(sql, params, (rs, rowNum) -> {
            Long id = rs.getLong("id");
            return mapOrder(
                    id,
                    rs.getString("order_no"),
                    rs.getLong("patient_id"),
                    rs.getString("patient_uhid"),
                    rs.getString("patient_name"),
                    rs.getObject("encounter_id") == null ? null : rs.getLong("encounter_id"),
                    rs.getString("encounter_type"),
                    rs.getString("source"),
                    rs.getString("department"),
                    rs.getString("priority"),
                    rs.getString("status"),
                    rs.getString("clinical_notes"),
                    parseJsonArray(rs.getString("diagnosis_codes_json")),
                    rs.getString("payer_context_id"),
                    rs.getTimestamp("preferred_at") == null ? null : rs.getTimestamp("preferred_at").toLocalDateTime(),
                    rs.getTimestamp("due_at") == null ? null : rs.getTimestamp("due_at").toLocalDateTime(),
                    parseJsonArray(rs.getString("flags_json")),
                    rs.getString("accession_no"),
                    rs.getString("barcode_no"),
                    rs.getString("billing_authorization_status"),
                    rs.getInt("billing_release_blocked") == 1,
                    rs.getObject("invoice_id") == null ? null : rs.getLong("invoice_id"),
                    findOrderLines(tenantId, hospitalId, branchId, id),
                    rs.getObject("created_by") == null ? null : rs.getLong("created_by"),
                    rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
            );
        });

        if (rows.isEmpty()) {
            throw ApiException.notFound("Diagnostic order not found.");
        }

        return rows.get(0);
    }

    public void updateOrderStatus(Long tenantId,
                                  Long hospitalId,
                                  String branchId,
                                  Long orderId,
                                  String status,
                                  Long userId) {

        String sql = """
                UPDATE diagnostic_orders
                SET status = :status,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :orderId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("orderId", orderId)
                .addValue("status", status)
                .addValue("updatedBy", userId);

        int updated = jdbc.update(sql, params);

        if (updated == 0) {
            throw ApiException.notFound("Diagnostic order not found.");
        }

        String lineSql = """
                UPDATE diagnostic_order_lines
                SET status = :status,
                    updated_at = NOW()
                WHERE order_id = :orderId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(lineSql, params);
    }

    public void insertStatusHistory(Long tenantId,
                                    Long hospitalId,
                                    String branchId,
                                    Long orderId,
                                    String fromStatus,
                                    String toStatus,
                                    String reason,
                                    String notes,
                                    Long userId,
                                    String actorRole,
                                    String requestId) {

        String sql = """
                INSERT INTO diagnostic_order_status_history (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    order_id,
                    from_status,
                    to_status,
                    reason,
                    notes,
                    actor_user_id,
                    actor_role,
                    request_id
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :orderId,
                    :fromStatus,
                    :toStatus,
                    :reason,
                    :notes,
                    :actorUserId,
                    :actorRole,
                    :requestId
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("orderId", orderId)
                .addValue("fromStatus", fromStatus)
                .addValue("toStatus", toStatus)
                .addValue("reason", reason)
                .addValue("notes", notes)
                .addValue("actorUserId", userId)
                .addValue("actorRole", actorRole)
                .addValue("requestId", requestId);

        jdbc.update(sql, params);
    }

    public BillingAuthorizationResponse createBillingAuthorization(Long tenantId,
                                                                  Long hospitalId,
                                                                  String branchId,
                                                                  Long orderId,
                                                                  Long userId,
                                                                  String actorRole,
                                                                  BillingAuthorizationRequest request) {

        String authorizationStatus;
        boolean releaseBlocked;

        if ("Approve".equals(request.action())) {
            authorizationStatus = "Approved";
            releaseBlocked = false;
        } else if ("Override".equals(request.action())) {
            authorizationStatus = "OverrideApproved";
            releaseBlocked = false;
        } else {
            authorizationStatus = "Blocked";
            releaseBlocked = true;
        }

        String insertSql = """
                INSERT INTO diagnostic_billing_authorizations (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    order_id,
                    action,
                    authorization_status,
                    release_blocked,
                    reason,
                    actor_user_id,
                    actor_role
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :orderId,
                    :action,
                    :authorizationStatus,
                    :releaseBlocked,
                    :reason,
                    :actorUserId,
                    :actorRole
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("orderId", orderId)
                .addValue("action", request.action())
                .addValue("authorizationStatus", authorizationStatus)
                .addValue("releaseBlocked", releaseBlocked ? 1 : 0)
                .addValue("reason", request.reason())
                .addValue("actorUserId", userId)
                .addValue("actorRole", actorRole);

        jdbc.update(insertSql, params);

        String updateSql = """
                UPDATE diagnostic_orders
                SET billing_authorization_status = :authorizationStatus,
                    billing_release_blocked = :releaseBlocked,
                    updated_by = :updatedBy,
                    updated_at = NOW()
                WHERE id = :orderId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        jdbc.update(updateSql, params.addValue("updatedBy", userId));

        return new BillingAuthorizationResponse(
                orderId,
                request.action(),
                authorizationStatus,
                releaseBlocked,
                request.reason(),
                LocalDateTime.now()
        );
    }

    public void insertDomainEvent(Long tenantId,
                                  Long hospitalId,
                                  String branchId,
                                  String eventType,
                                  String aggregateType,
                                  String aggregateId,
                                  Long actorUserId,
                                  String requestId,
                                  String payloadJson) {

        String sql = """
                INSERT INTO diagnostic_domain_events (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    event_type,
                    aggregate_type,
                    aggregate_id,
                    actor_user_id,
                    request_id,
                    payload_json
                ) VALUES (
                    :tenantId,
                    :hospitalId,
                    :branchId,
                    :eventType,
                    :aggregateType,
                    :aggregateId,
                    :actorUserId,
                    :requestId,
                    CAST(:payloadJson AS JSON)
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("eventType", eventType)
                .addValue("aggregateType", aggregateType)
                .addValue("aggregateId", aggregateId)
                .addValue("actorUserId", actorUserId)
                .addValue("requestId", requestId)
                .addValue("payloadJson", payloadJson);

        jdbc.update(sql, params);
    }

    private List<DiagnosticOrderLineResponse> findOrderLines(Long tenantId,
                                                             Long hospitalId,
                                                             String branchId,
                                                             Long orderId) {

        String sql = """
                SELECT
                    id,
                    service_id,
                    service_code,
                    service_name,
                    department,
                    modality,
                    status
                FROM diagnostic_order_lines
                WHERE order_id = :orderId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                ORDER BY id ASC
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("orderId", orderId);

        return jdbc.query(sql, params, (rs, rowNum) -> new DiagnosticOrderLineResponse(
                rs.getLong("id"),
                rs.getLong("service_id"),
                rs.getString("service_code"),
                rs.getString("service_name"),
                rs.getString("department"),
                rs.getString("modality"),
                rs.getString("status")
        ));
    }

    private DiagnosticOrderResponse mapOrder(Long orderId,
                                             String orderNo,
                                             Long patientId,
                                             String patientUhid,
                                             String patientName,
                                             Long encounterId,
                                             String encounterType,
                                             String source,
                                             String department,
                                             String priority,
                                             String status,
                                             String clinicalNotes,
                                             List<String> diagnosisCodes,
                                             String payerContextId,
                                             LocalDateTime preferredAt,
                                             LocalDateTime dueAt,
                                             List<String> flags,
                                             String accessionNo,
                                             String barcodeNo,
                                             String billingAuthorizationStatus,
                                             Boolean billingReleaseBlocked,
                                             Long invoiceId,
                                             List<DiagnosticOrderLineResponse> lines,
                                             Long createdBy,
                                             LocalDateTime createdAt) {

        return new DiagnosticOrderResponse(
                orderId,
                orderNo,
                patientId,
                patientUhid,
                patientName,
                encounterId,
                encounterType,
                source,
                department,
                priority,
                status,
                clinicalNotes,
                diagnosisCodes,
                payerContextId,
                preferredAt,
                dueAt,
                flags,
                accessionNo,
                barcodeNo,
                billingAuthorizationStatus,
                billingReleaseBlocked,
                invoiceId,
                lines,
                createdBy,
                createdAt
        );
    }

    private DiagnosticOrderResponse withLines(DiagnosticOrderResponse order,
                                              List<DiagnosticOrderLineResponse> lines) {
        return new DiagnosticOrderResponse(
                order.orderId(),
                order.orderNo(),
                order.patientId(),
                order.patientUhid(),
                order.patientName(),
                order.encounterId(),
                order.encounterType(),
                order.source(),
                order.department(),
                order.priority(),
                order.status(),
                order.clinicalNotes(),
                order.diagnosisCodes(),
                order.payerContextId(),
                order.preferredAt(),
                order.dueAt(),
                order.flags(),
                order.accessionNo(),
                order.barcodeNo(),
                order.billingAuthorizationStatus(),
                order.billingReleaseBlocked(),
                order.invoiceId(),
                lines,
                order.createdBy(),
                order.createdAt()
        );
    }

    private void appendOrderFilters(StringBuilder sql,
                                    String department,
                                    String status,
                                    String priority,
                                    String source,
                                    String search) {

        if (department != null && !department.isBlank()) {
            sql.append(" AND department = :department ");
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status ");
        }

        if (priority != null && !priority.isBlank()) {
            sql.append(" AND priority = :priority ");
        }

        if (source != null && !source.isBlank()) {
            sql.append(" AND source = :source ");
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        order_no LIKE :search
                        OR patient_uhid LIKE :search
                        OR patient_name LIKE :search
                        OR accession_no LIKE :search
                        OR barcode_no LIKE :search
                    )
                    """);
        }
    }

    private MapSqlParameterSource orderFilterParams(Long tenantId,
                                                    Long hospitalId,
                                                    String branchId,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String department,
                                                    String status,
                                                    String priority,
                                                    String source,
                                                    String search) {

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);

        if (department != null && !department.isBlank()) {
            params.addValue("department", department);
        }

        if (status != null && !status.isBlank()) {
            params.addValue("status", status);
        }

        if (priority != null && !priority.isBlank()) {
            params.addValue("priority", priority);
        }

        if (source != null && !source.isBlank()) {
            params.addValue("source", source);
        }

        if (search != null && !search.isBlank()) {
            params.addValue("search", "%" + search.trim() + "%");
        }

        return params;
    }

    private MapSqlParameterSource tenantHospitalParams(Long tenantId, Long hospitalId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId);
    }

    private MapSqlParameterSource baseParams(Long tenantId, Long hospitalId, String branchId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("branchId", branchId);
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        String cleaned = json
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();

        if (cleaned.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}