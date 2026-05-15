package com.plasmit.diagnostics.payment.billingdiagnostics.repository;

import com.plasmit.diagnostics.payment.billingdiagnostics.dto.request.DiagnosticInvoiceCreateRequest;
import com.plasmit.diagnostics.payment.billingdiagnostics.dto.response.DiagnosticHandoffResponse;
import com.plasmit.diagnostics.payment.billingdiagnostics.dto.response.DiagnosticServiceResponse;
import com.plasmit.diagnostics.payment.common.exception.ApiException;
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
public class BillingDiagnosticRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public BillingDiagnosticRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
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

    public List<String> findPayerTypes(Long tenantId, Long hospitalId, String branchId) {
        String sql = """
                SELECT DISTINCT payer_type
                FROM diagnostic_service_tariffs
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                  AND status = 'ACTIVE'
                ORDER BY payer_type
                """;

        return jdbc.queryForList(sql, baseParams(tenantId, hospitalId, branchId), String.class);
    }

    public List<DiagnosticServiceResponse> searchServices(Long tenantId,
                                                          Long hospitalId,
                                                          String branchId,
                                                          String payerType,
                                                          String department,
                                                          String modality,
                                                          String search,
                                                          Integer page,
                                                          Integer limit) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    s.id AS service_id,
                    s.service_code,
                    s.service_name,
                    s.department,
                    s.modality,
                    s.sample_type,
                    s.turnaround_minutes,
                    s.requires_approval,
                    t.price_paise,
                    t.tax_percent
                FROM diagnostic_services s
                INNER JOIN diagnostic_service_tariffs t ON t.service_id = s.id
                WHERE s.tenant_id = :tenantId
                  AND s.hospital_id = :hospitalId
                  AND s.branch_id = :branchId
                  AND t.tenant_id = :tenantId
                  AND t.hospital_id = :hospitalId
                  AND t.branch_id = :branchId
                  AND s.is_deleted = 0
                  AND t.is_deleted = 0
                  AND s.is_active = 1
                  AND t.status = 'ACTIVE'
                  AND t.payer_type = :payerType
                  AND t.effective_from <= CURRENT_DATE()
                  AND (t.effective_to IS NULL OR t.effective_to >= CURRENT_DATE())
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("payerType", payerType);

        if (department != null && !department.isBlank()) {
            sql.append(" AND s.department = :department ");
            params.addValue("department", department);
        }

        if (modality != null && !modality.isBlank()) {
            sql.append(" AND s.modality = :modality ");
            params.addValue("modality", modality);
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (
                        s.service_code LIKE :search
                        OR s.service_name LIKE :search
                        OR s.department LIKE :search
                        OR s.modality LIKE :search
                    )
                    """);
            params.addValue("search", "%" + search.trim() + "%");
        }

        sql.append(" ORDER BY s.service_name ASC LIMIT :limit OFFSET :offset ");
        params.addValue("limit", limit);
        params.addValue("offset", (page - 1) * limit);

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> new DiagnosticServiceResponse(
                rs.getLong("service_id"),
                rs.getString("service_code"),
                rs.getString("service_name"),
                rs.getString("department"),
                rs.getString("modality"),
                rs.getString("sample_type"),
                rs.getInt("turnaround_minutes"),
                rs.getInt("requires_approval") == 1,
                rs.getLong("price_paise"),
                rs.getDouble("tax_percent")
        ));
    }

    public DiagnosticServiceResponse findServiceById(Long tenantId,
                                                     Long hospitalId,
                                                     String branchId,
                                                     Long serviceId,
                                                     String payerType) {

        List<DiagnosticServiceResponse> rows = searchServicesByIds(
                tenantId, hospitalId, branchId, List.of(serviceId), payerType
        );

        if (rows.isEmpty()) {
            throw ApiException.notFound("Diagnostic service not found.");
        }

        return rows.get(0);
    }

    public List<DiagnosticServiceResponse> searchServicesByIds(Long tenantId,
                                                               Long hospitalId,
                                                               String branchId,
                                                               List<Long> serviceIds,
                                                               String payerType) {

        String sql = """
                SELECT
                    s.id AS service_id,
                    s.service_code,
                    s.service_name,
                    s.department,
                    s.modality,
                    s.sample_type,
                    s.turnaround_minutes,
                    s.requires_approval,
                    t.price_paise,
                    t.tax_percent
                FROM diagnostic_services s
                INNER JOIN diagnostic_service_tariffs t ON t.service_id = s.id
                WHERE s.tenant_id = :tenantId
                  AND s.hospital_id = :hospitalId
                  AND s.branch_id = :branchId
                  AND t.tenant_id = :tenantId
                  AND t.hospital_id = :hospitalId
                  AND t.branch_id = :branchId
                  AND s.id IN (:serviceIds)
                  AND t.payer_type = :payerType
                  AND s.is_deleted = 0
                  AND t.is_deleted = 0
                  AND s.is_active = 1
                  AND t.status = 'ACTIVE'
                  AND t.effective_from <= CURRENT_DATE()
                  AND (t.effective_to IS NULL OR t.effective_to >= CURRENT_DATE())
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("serviceIds", serviceIds)
                .addValue("payerType", payerType);

        return jdbc.query(sql, params, (rs, rowNum) -> new DiagnosticServiceResponse(
                rs.getLong("service_id"),
                rs.getString("service_code"),
                rs.getString("service_name"),
                rs.getString("department"),
                rs.getString("modality"),
                rs.getString("sample_type"),
                rs.getInt("turnaround_minutes"),
                rs.getInt("requires_approval") == 1,
                rs.getLong("price_paise"),
                rs.getDouble("tax_percent")
        ));
    }

    public Long createInvoice(Long tenantId,
                              Long hospitalId,
                              String branchId,
                              Long userId,
                              DiagnosticInvoiceCreateRequest request,
                              String invoiceNo,
                              Long grossPaise,
                              Long discountPaise,
                              Long taxPaise,
                              Long netPaise,
                              Long paidPaise,
                              Long duePaise,
                              String paymentMethod,
                              String status) {

        String sql = """
                INSERT INTO hospital_invoices (
                    tenant_id, hospital_id, branch_id,
                    invoice_no, bill_date_time,
                    patient_id, patient_uhid, patient_name, patient_age_gender,
                    encounter_id, encounter_type,
                    payer_type, payer_name, authorization_no,
                    department, service_name,
                    gross_paise, discount_paise, tax_paise, net_paise,
                    paid_paise, due_paise, refund_paise,
                    payment_method, status,
                    cashier_id, cashier_name,
                    settlement_status,
                    created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :invoiceNo, :billDateTime,
                    :patientId, :patientUhid, :patientName, :patientAgeGender,
                    :encounterId, :encounterType,
                    :payerType, :payerName, :authorizationNo,
                    'Diagnostics', 'Diagnostic Services',
                    :grossPaise, :discountPaise, :taxPaise, :netPaise,
                    :paidPaise, :duePaise, 0,
                    :paymentMethod, :status,
                    :cashierId, :cashierName,
                    'Pending',
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("invoiceNo", invoiceNo)
                .addValue("billDateTime", Timestamp.valueOf(LocalDateTime.now()))
                .addValue("patientId", request.patientId())
                .addValue("patientUhid", request.patientUhid())
                .addValue("patientName", request.patientName())
                .addValue("patientAgeGender", request.patientAgeGender())
                .addValue("encounterId", request.encounterId())
                .addValue("encounterType", request.encounterType())
                .addValue("payerType", request.payerType())
                .addValue("payerName", request.payerName())
                .addValue("authorizationNo", request.authorizationNo())
                .addValue("grossPaise", grossPaise)
                .addValue("discountPaise", discountPaise)
                .addValue("taxPaise", taxPaise)
                .addValue("netPaise", netPaise)
                .addValue("paidPaise", paidPaise)
                .addValue("duePaise", duePaise)
                .addValue("paymentMethod", paymentMethod)
                .addValue("status", status)
                .addValue("cashierId", userId)
                .addValue("cashierName", "Billing User")
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public Long createInvoiceLine(Long tenantId,
                                  Long hospitalId,
                                  String branchId,
                                  Long invoiceId,
                                  DiagnosticServiceResponse service,
                                  Integer quantity,
                                  Long grossPaise,
                                  Long discountPaise,
                                  Long taxPaise,
                                  Long netPaise) {

        String sql = """
                INSERT INTO hospital_invoice_lines (
                    tenant_id, hospital_id, branch_id,
                    invoice_id,
                    service_id, service_code, service_name,
                    department, billing_route,
                    quantity,
                    gross_paise, discount_paise, tax_paise, net_paise
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :invoiceId,
                    :serviceId, :serviceCode, :serviceName,
                    :department, :billingRoute,
                    :quantity,
                    :grossPaise, :discountPaise, :taxPaise, :netPaise
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("invoiceId", invoiceId)
                .addValue("serviceId", String.valueOf(service.serviceId()))
                .addValue("serviceCode", service.serviceCode())
                .addValue("serviceName", service.serviceName())
                .addValue("department", service.department())
                .addValue("billingRoute", service.department())
                .addValue("quantity", quantity)
                .addValue("grossPaise", grossPaise)
                .addValue("discountPaise", discountPaise)
                .addValue("taxPaise", taxPaise)
                .addValue("netPaise", netPaise);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void createPaymentTransaction(Long tenantId,
                                         Long hospitalId,
                                         String branchId,
                                         Long invoiceId,
                                         String paymentMethod,
                                         Long amountPaise,
                                         String referenceNo,
                                         Long userId) {

        String sql = """
                INSERT INTO hospital_payment_transactions (
                    tenant_id, hospital_id, branch_id,
                    invoice_id,
                    payment_method,
                    amount_paise,
                    transaction_reference,
                    payment_status,
                    paid_at,
                    created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :invoiceId,
                    :paymentMethod,
                    :amountPaise,
                    :referenceNo,
                    'Success',
                    :paidAt,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("invoiceId", invoiceId)
                .addValue("paymentMethod", paymentMethod)
                .addValue("amountPaise", amountPaise)
                .addValue("referenceNo", referenceNo)
                .addValue("paidAt", Timestamp.valueOf(LocalDateTime.now()))
                .addValue("createdBy", userId);

        jdbc.update(sql, params);
    }

    public Long createHandoff(Long tenantId,
                              Long hospitalId,
                              String branchId,
                              Long invoiceId,
                              Long invoiceLineId,
                              Long serviceId,
                              String modality,
                              String route,
                              Long userId) {

        String sql = """
                INSERT INTO diagnostic_invoice_handoffs (
                    tenant_id, hospital_id, branch_id,
                    invoice_id, invoice_line_id, service_id,
                    modality, handoff_route,
                    handoff_status, attempts,
                    created_by
                ) VALUES (
                    :tenantId, :hospitalId, :branchId,
                    :invoiceId, :invoiceLineId, :serviceId,
                    :modality, :handoffRoute,
                    'Pending', 0,
                    :createdBy
                )
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("invoiceId", invoiceId)
                .addValue("invoiceLineId", invoiceLineId)
                .addValue("serviceId", serviceId)
                .addValue("modality", modality)
                .addValue("handoffRoute", route)
                .addValue("createdBy", userId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"id"});

        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public List<DiagnosticHandoffResponse> findHandoffs(Long tenantId,
                                                        Long hospitalId,
                                                        String branchId,
                                                        String status) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    h.id,
                    h.invoice_id,
                    h.service_id,
                    s.service_name,
                    h.modality,
                    h.handoff_route,
                    h.handoff_status,
                    h.attempts,
                    h.last_error,
                    h.created_at,
                    h.updated_at
                FROM diagnostic_invoice_handoffs h
                INNER JOIN diagnostic_services s ON s.id = h.service_id
                WHERE h.tenant_id = :tenantId
                  AND h.hospital_id = :hospitalId
                  AND h.branch_id = :branchId
                  AND h.is_deleted = 0
                """);

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId);

        if (status != null && !status.isBlank()) {
            sql.append(" AND h.handoff_status = :status ");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY h.created_at DESC ");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> new DiagnosticHandoffResponse(
                rs.getLong("id"),
                rs.getLong("invoice_id"),
                rs.getLong("service_id"),
                rs.getString("service_name"),
                rs.getString("modality"),
                rs.getString("handoff_route"),
                rs.getString("handoff_status"),
                rs.getInt("attempts"),
                rs.getString("last_error"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime()
        ));
    }

    public void retryHandoff(Long tenantId, Long hospitalId, String branchId, Long handoffId) {
        String sql = """
                UPDATE diagnostic_invoice_handoffs
                SET handoff_status = 'Pending',
                    attempts = attempts + 1,
                    last_error = NULL,
                    updated_at = NOW()
                WHERE id = :handoffId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        int updated = jdbc.update(sql, baseParams(tenantId, hospitalId, branchId).addValue("handoffId", handoffId));

        if (updated == 0) {
            throw ApiException.notFound("Diagnostic handoff not found.");
        }
    }

    public void cancelHandoff(Long tenantId, Long hospitalId, String branchId, Long handoffId, String reason) {
        String sql = """
                UPDATE diagnostic_invoice_handoffs
                SET handoff_status = 'Cancelled',
                    cancelled_reason = :reason,
                    updated_at = NOW()
                WHERE id = :handoffId
                  AND tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND is_deleted = 0
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, branchId)
                .addValue("handoffId", handoffId)
                .addValue("reason", reason);

        int updated = jdbc.update(sql, params);

        if (updated == 0) {
            throw ApiException.notFound("Diagnostic handoff not found.");
        }
    }

    private MapSqlParameterSource baseParams(Long tenantId, Long hospitalId, String branchId) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("branchId", branchId);
    }
}