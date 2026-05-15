package com.plasmit.diagnostics.payment.paymentreports.repository;

import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentFilterOptionResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentLedgerRowResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.CollectionMixResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.DepartmentBillingResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.DueAgingResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.SettlementExceptionResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.request.ReconciliationRunRequest;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.ReconciliationRunResponse;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import com.plasmit.diagnostics.payment.common.exception.ApiException;
import com.plasmit.diagnostics.payment.paymentreports.dto.request.ExportJobRequest;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.ExportJobResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentAuditResponse;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.Objects;

import java.time.LocalDate;

@Repository
public class PaymentReportRepository {

    private static final Logger log = LoggerFactory.getLogger(PaymentReportRepository.class);

    private final NamedParameterJdbcTemplate jdbc;

    public PaymentReportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PaymentSummaryResponse getSummary(Long tenantId,
                                             Long hospitalId,
                                             String branchId,
                                             LocalDate fromDate,
                                             LocalDate toDate) {

        String sql = """
                SELECT
                    COALESCE(SUM(gross_paise), 0) AS gross_billing_paise,
                    COALESCE(SUM(discount_paise), 0) AS discount_paise,
                    COALESCE(SUM(tax_paise), 0) AS tax_paise,
                    COALESCE(SUM(net_paise), 0) AS net_billing_paise,
                    COALESCE(SUM(paid_paise), 0) AS collected_paise,
                    COALESCE(SUM(due_paise), 0) AS outstanding_paise,
                    COALESCE(SUM(refund_paise), 0) AS refund_paise,
                    COUNT(*) AS invoice_count,
                    COALESCE(SUM(CASE WHEN status = 'Paid' THEN 1 ELSE 0 END), 0) AS paid_invoice_count,
                    COALESCE(SUM(CASE WHEN status = 'Partial' THEN 1 ELSE 0 END), 0) AS partial_invoice_count,
                    COALESCE(SUM(CASE WHEN status = 'Due' THEN 1 ELSE 0 END), 0) AS due_invoice_count
                FROM hospital_invoices
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND branch_id = :branchId
                  AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                """;

        MapSqlParameterSource params = baseParams(tenantId, hospitalId, fromDate, toDate)
                .addValue("branchId", branchId);

        return jdbc.queryForObject(sql, params, (rs, rowNum) -> {
            PaymentSummaryResponse response = new PaymentSummaryResponse();
            response.setGrossBillingPaise(rs.getLong("gross_billing_paise"));
            response.setDiscountPaise(rs.getLong("discount_paise"));
            response.setTaxPaise(rs.getLong("tax_paise"));
            response.setNetBillingPaise(rs.getLong("net_billing_paise"));
            response.setCollectedPaise(rs.getLong("collected_paise"));
            response.setOutstandingPaise(rs.getLong("outstanding_paise"));
            response.setRefundPaise(rs.getLong("refund_paise"));
            response.setInvoiceCount(rs.getLong("invoice_count"));
            response.setPaidInvoiceCount(rs.getLong("paid_invoice_count"));
            response.setPartialInvoiceCount(rs.getLong("partial_invoice_count"));
            response.setDueInvoiceCount(rs.getLong("due_invoice_count"));
            return response;
        });
    }

    public List<PaymentFilterOptionResponse> findBranches(Long tenantId,
                                                          Long hospitalId,
                                                          LocalDate fromDate,
                                                          LocalDate toDate) {

        String sql = """
                SELECT DISTINCT branch_id
                FROM hospital_invoices
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                  AND branch_id IS NOT NULL
                  AND branch_id <> ''
                ORDER BY branch_id
                """;

        return jdbc.query(sql, baseParams(tenantId, hospitalId, fromDate, toDate), (rs, rowNum) ->
                new PaymentFilterOptionResponse(
                        rs.getString("branch_id"),
                        formatBranchName(rs.getString("branch_id"))
                )
        );
    }

    public List<String> findDepartments(Long tenantId,
                                        Long hospitalId,
                                        LocalDate fromDate,
                                        LocalDate toDate) {

        String sql = """
                SELECT DISTINCT department
                FROM hospital_invoices
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                  AND department IS NOT NULL
                  AND department <> ''
                ORDER BY department
                """;

        return jdbc.queryForList(sql, baseParams(tenantId, hospitalId, fromDate, toDate), String.class);
    }

    public List<String> findPayerTypes(Long tenantId,
                                       Long hospitalId,
                                       LocalDate fromDate,
                                       LocalDate toDate) {

        String sql = """
                SELECT DISTINCT payer_type
                FROM hospital_invoices
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                  AND payer_type IS NOT NULL
                  AND payer_type <> ''
                ORDER BY payer_type
                """;

        return jdbc.queryForList(sql, baseParams(tenantId, hospitalId, fromDate, toDate), String.class);
    }

    public List<String> findPaymentMethods(Long tenantId,
                                           Long hospitalId,
                                           LocalDate fromDate,
                                           LocalDate toDate) {

        String sql = """
                SELECT DISTINCT payment_method
                FROM hospital_invoices
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                  AND payment_method IS NOT NULL
                  AND payment_method <> ''
                ORDER BY payment_method
                """;

        return jdbc.queryForList(sql, baseParams(tenantId, hospitalId, fromDate, toDate), String.class);
    }

    public List<String> findStatuses(Long tenantId,
                                     Long hospitalId,
                                     LocalDate fromDate,
                                     LocalDate toDate) {

        String sql = """
                SELECT DISTINCT status
                FROM hospital_invoices
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                  AND status IS NOT NULL
                  AND status <> ''
                ORDER BY status
                """;

        return jdbc.queryForList(sql, baseParams(tenantId, hospitalId, fromDate, toDate), String.class);
    }

    public List<String> findSettlementStatuses(Long tenantId,
                                               Long hospitalId,
                                               LocalDate fromDate,
                                               LocalDate toDate) {

        String sql = """
                SELECT DISTINCT settlement_status
                FROM hospital_invoices
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                  AND settlement_status IS NOT NULL
                  AND settlement_status <> ''
                ORDER BY settlement_status
                """;

        return jdbc.queryForList(sql, baseParams(tenantId, hospitalId, fromDate, toDate), String.class);
    }

    public List<PaymentFilterOptionResponse> findCashiers(Long tenantId,
                                                          Long hospitalId,
                                                          LocalDate fromDate,
                                                          LocalDate toDate) {

        String sql = """
                SELECT DISTINCT cashier_id, cashier_name
                FROM hospital_invoices
                WHERE tenant_id = :tenantId
                  AND hospital_id = :hospitalId
                  AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
                  AND is_deleted = 0
                  AND cashier_id IS NOT NULL
                  AND cashier_name IS NOT NULL
                  AND cashier_name <> ''
                ORDER BY cashier_name
                """;

        return jdbc.query(sql, baseParams(tenantId, hospitalId, fromDate, toDate), (rs, rowNum) ->
                new PaymentFilterOptionResponse(
                        String.valueOf(rs.getLong("cashier_id")),
                        rs.getString("cashier_name")
                )
        );
    }

    private MapSqlParameterSource baseParams(Long tenantId,
                                             Long hospitalId,
                                             LocalDate fromDate,
                                             LocalDate toDate) {
        return new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("hospitalId", hospitalId)
                .addValue("fromDate", fromDate)
                .addValue("toDate", toDate);
    }

    private String formatBranchName(String branchId) {
        if ("br_main".equalsIgnoreCase(branchId)) {
            return "Main Branch";
        }

        return branchId;
    }
    
    public List<PaymentLedgerRowResponse> findLedgerRows(Long tenantId,
            Long hospitalId,
            String branchId,
            LocalDate fromDate,
            LocalDate toDate,
            String department,
            String payerType,
            String status,
            String paymentMethod,
            String settlementStatus,
            Long cashierId,
            String search,
            Integer page,
            Integer limit,
            String sortBy,
            String sortOrder) {

StringBuilder sql = new StringBuilder("""
SELECT
id,
invoice_no,
bill_date_time,
branch_id,
patient_uhid,
patient_name,
patient_age_gender,
encounter_id,
encounter_type,
payer_type,
payer_name,
authorization_no,
department,
service_name,
gross_paise,
discount_paise,
tax_paise,
net_paise,
paid_paise,
due_paise,
refund_paise,
payment_method,
status,
cashier_id,
cashier_name,
settlement_status,
settlement_batch_id,
settled_at
FROM hospital_invoices
WHERE tenant_id = :tenantId
AND hospital_id = :hospitalId
AND branch_id = :branchId
AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
AND is_deleted = 0
""");

MapSqlParameterSource params = ledgerParams(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
department,
payerType,
status,
paymentMethod,
settlementStatus,
cashierId,
search
);

appendLedgerFilters(sql, department, payerType, status, paymentMethod, settlementStatus, cashierId, search);

sql.append(" ORDER BY ")
.append(resolveSortColumn(sortBy))
.append(" ")
.append(resolveSortOrder(sortOrder));

sql.append(" LIMIT :limit OFFSET :offset");

params.addValue("limit", limit);
params.addValue("offset", (page - 1) * limit);

log.debug("Executing payment ledger query. tenantId={} hospitalId={} branchId={} page={} limit={}",
tenantId, hospitalId, branchId, page, limit);

return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
PaymentLedgerRowResponse row = new PaymentLedgerRowResponse();

row.setInvoiceId(rs.getLong("id"));
row.setInvoiceNo(rs.getString("invoice_no"));
row.setBillDateTime(rs.getTimestamp("bill_date_time").toLocalDateTime());
row.setBranchId(rs.getString("branch_id"));

row.setPatientUhid(rs.getString("patient_uhid"));
row.setPatientName(rs.getString("patient_name"));
row.setPatientAgeGender(rs.getString("patient_age_gender"));

long encounterId = rs.getLong("encounter_id");
row.setEncounterId(rs.wasNull() ? null : encounterId);
row.setEncounterType(rs.getString("encounter_type"));

row.setPayerType(rs.getString("payer_type"));
row.setPayerName(rs.getString("payer_name"));
row.setAuthorizationNo(rs.getString("authorization_no"));

row.setDepartment(rs.getString("department"));
row.setServiceName(rs.getString("service_name"));

row.setGrossPaise(rs.getLong("gross_paise"));
row.setDiscountPaise(rs.getLong("discount_paise"));
row.setTaxPaise(rs.getLong("tax_paise"));
row.setNetPaise(rs.getLong("net_paise"));
row.setPaidPaise(rs.getLong("paid_paise"));
row.setDuePaise(rs.getLong("due_paise"));
row.setRefundPaise(rs.getLong("refund_paise"));

row.setPaymentMethod(rs.getString("payment_method"));
row.setStatus(rs.getString("status"));

long cashierIdValue = rs.getLong("cashier_id");
row.setCashierId(rs.wasNull() ? null : cashierIdValue);
row.setCashierName(rs.getString("cashier_name"));

row.setSettlementStatus(rs.getString("settlement_status"));
row.setSettlementBatchId(rs.getString("settlement_batch_id"));

if (rs.getTimestamp("settled_at") != null) {
row.setSettledAt(rs.getTimestamp("settled_at").toLocalDateTime());
}

return row;
});
}

public Long countLedgerRows(Long tenantId,
Long hospitalId,
String branchId,
LocalDate fromDate,
LocalDate toDate,
String department,
String payerType,
String status,
String paymentMethod,
String settlementStatus,
Long cashierId,
String search) {

StringBuilder sql = new StringBuilder("""
SELECT COUNT(*)
FROM hospital_invoices
WHERE tenant_id = :tenantId
AND hospital_id = :hospitalId
AND branch_id = :branchId
AND DATE(bill_date_time) BETWEEN :fromDate AND :toDate
AND is_deleted = 0
""");

MapSqlParameterSource params = ledgerParams(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
department,
payerType,
status,
paymentMethod,
settlementStatus,
cashierId,
search
);

appendLedgerFilters(sql, department, payerType, status, paymentMethod, settlementStatus, cashierId, search);

Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
return total == null ? 0L : total;
}

private MapSqlParameterSource ledgerParams(Long tenantId,
  Long hospitalId,
  String branchId,
  LocalDate fromDate,
  LocalDate toDate,
  String department,
  String payerType,
  String status,
  String paymentMethod,
  String settlementStatus,
  Long cashierId,
  String search) {

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId)
.addValue("fromDate", fromDate)
.addValue("toDate", toDate);

if (department != null && !department.isBlank()) {
params.addValue("department", department);
}

if (payerType != null && !payerType.isBlank()) {
params.addValue("payerType", payerType);
}

if (status != null && !status.isBlank()) {
params.addValue("status", status);
}

if (paymentMethod != null && !paymentMethod.isBlank()) {
params.addValue("paymentMethod", paymentMethod);
}

if (settlementStatus != null && !settlementStatus.isBlank()) {
params.addValue("settlementStatus", settlementStatus);
}

if (cashierId != null) {
params.addValue("cashierId", cashierId);
}

if (search != null && !search.isBlank()) {
params.addValue("search", "%" + search.trim() + "%");
}

return params;
}

private void appendLedgerFilters(StringBuilder sql,
String department,
String payerType,
String status,
String paymentMethod,
String settlementStatus,
Long cashierId,
String search) {

if (department != null && !department.isBlank()) {
sql.append(" AND department = :department");
}

if (payerType != null && !payerType.isBlank()) {
sql.append(" AND payer_type = :payerType");
}

if (status != null && !status.isBlank()) {
sql.append(" AND status = :status");
}

if (paymentMethod != null && !paymentMethod.isBlank()) {
sql.append(" AND payment_method = :paymentMethod");
}

if (settlementStatus != null && !settlementStatus.isBlank()) {
sql.append(" AND settlement_status = :settlementStatus");
}

if (cashierId != null) {
sql.append(" AND cashier_id = :cashierId");
}

if (search != null && !search.isBlank()) {
sql.append("""
AND (
invoice_no LIKE :search
OR patient_uhid LIKE :search
OR patient_name LIKE :search
OR payer_name LIKE :search
OR service_name LIKE :search
OR authorization_no LIKE :search
)
""");
}
}

private String resolveSortColumn(String sortBy) {
if (sortBy == null || sortBy.isBlank()) {
return "bill_date_time";
}

return switch (sortBy) {
case "billDateTime" -> "bill_date_time";
case "invoiceNo" -> "invoice_no";
case "patientName" -> "patient_name";
case "department" -> "department";
case "payerType" -> "payer_type";
case "paymentMethod" -> "payment_method";
case "status" -> "status";
case "settlementStatus" -> "settlement_status";
case "netPaise" -> "net_paise";
case "paidPaise" -> "paid_paise";
case "duePaise" -> "due_paise";
default -> "bill_date_time";
};
}

private String resolveSortOrder(String sortOrder) {
if ("asc".equalsIgnoreCase(sortOrder)) {
return "ASC";
}
return "DESC";
}
public List<CollectionMixResponse> findCollectionMix(Long tenantId,
        Long hospitalId,
        String branchId,
        LocalDate fromDate,
        LocalDate toDate,
        String department,
        String payerType,
        String status,
        Long cashierId) {

StringBuilder sql = new StringBuilder("""
SELECT
p.payment_method AS method,
COALESCE(SUM(p.amount_paise), 0) AS collected_paise,
COUNT(p.id) AS transaction_count
FROM hospital_payment_transactions p
INNER JOIN hospital_invoices i ON i.id = p.invoice_id
WHERE p.tenant_id = :tenantId
AND p.hospital_id = :hospitalId
AND p.branch_id = :branchId
AND i.tenant_id = :tenantId
AND i.hospital_id = :hospitalId
AND i.branch_id = :branchId
AND DATE(i.bill_date_time) BETWEEN :fromDate AND :toDate
AND p.is_deleted = 0
AND i.is_deleted = 0
AND p.payment_status = 'Success'
""");

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId)
.addValue("fromDate", fromDate)
.addValue("toDate", toDate);

if (department != null && !department.isBlank()) {
sql.append(" AND i.department = :department ");
params.addValue("department", department);
}

if (payerType != null && !payerType.isBlank()) {
sql.append(" AND i.payer_type = :payerType ");
params.addValue("payerType", payerType);
}

if (status != null && !status.isBlank()) {
sql.append(" AND i.status = :status ");
params.addValue("status", status);
}

if (cashierId != null) {
sql.append(" AND i.cashier_id = :cashierId ");
params.addValue("cashierId", cashierId);
}

sql.append("""
GROUP BY p.payment_method
ORDER BY collected_paise DESC
""");

return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
CollectionMixResponse response = new CollectionMixResponse();
response.setMethod(rs.getString("method"));
response.setCollectedPaise(rs.getLong("collected_paise"));
response.setTransactionCount(rs.getLong("transaction_count"));
response.setPercentage(0.0);
return response;
});
}
public List<DepartmentBillingResponse> findDepartmentBilling(Long tenantId,
        Long hospitalId,
        String branchId,
        LocalDate fromDate,
        LocalDate toDate,
        String payerType,
        String status,
        String paymentMethod,
        Long cashierId) {

StringBuilder sql = new StringBuilder("""
SELECT
i.department AS department,
COALESCE(SUM(i.gross_paise), 0) AS gross_paise,
COALESCE(SUM(i.net_paise), 0) AS net_paise,
COALESCE(SUM(i.paid_paise), 0) AS collected_paise,
COALESCE(SUM(i.due_paise), 0) AS due_paise,
COALESCE(SUM(i.refund_paise), 0) AS refund_paise,
COUNT(i.id) AS invoice_count
FROM hospital_invoices i
WHERE i.tenant_id = :tenantId
AND i.hospital_id = :hospitalId
AND i.branch_id = :branchId
AND DATE(i.bill_date_time) BETWEEN :fromDate AND :toDate
AND i.is_deleted = 0
AND i.department IS NOT NULL
AND i.department <> ''
""");

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId)
.addValue("fromDate", fromDate)
.addValue("toDate", toDate);

if (payerType != null && !payerType.isBlank()) {
sql.append(" AND i.payer_type = :payerType ");
params.addValue("payerType", payerType);
}

if (status != null && !status.isBlank()) {
sql.append(" AND i.status = :status ");
params.addValue("status", status);
}

if (paymentMethod != null && !paymentMethod.isBlank()) {
sql.append(" AND i.payment_method = :paymentMethod ");
params.addValue("paymentMethod", paymentMethod);
}

if (cashierId != null) {
sql.append(" AND i.cashier_id = :cashierId ");
params.addValue("cashierId", cashierId);
}

sql.append("""
GROUP BY i.department
ORDER BY net_paise DESC
""");

return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
DepartmentBillingResponse response = new DepartmentBillingResponse();

response.setDepartment(rs.getString("department"));
response.setGrossPaise(rs.getLong("gross_paise"));
response.setNetPaise(rs.getLong("net_paise"));
response.setCollectedPaise(rs.getLong("collected_paise"));
response.setDuePaise(rs.getLong("due_paise"));
response.setRefundPaise(rs.getLong("refund_paise"));
response.setInvoiceCount(rs.getLong("invoice_count"));

return response;
});
}
public List<DueAgingResponse> findDueAging(Long tenantId,
        Long hospitalId,
        String branchId,
        LocalDate fromDate,
        LocalDate toDate,
        String department,
        String payerType,
        String status,
        String paymentMethod,
        Long cashierId) {

StringBuilder sql = new StringBuilder("""
SELECT
i.payer_type AS payer_type,

COALESCE(SUM(
CASE
WHEN DATEDIFF(:toDate, DATE(i.bill_date_time)) = 0
THEN i.due_paise ELSE 0
END
), 0) AS current_paise,

COALESCE(SUM(
CASE
WHEN DATEDIFF(:toDate, DATE(i.bill_date_time)) BETWEEN 1 AND 7
THEN i.due_paise ELSE 0
END
), 0) AS days_1_to_7_paise,

COALESCE(SUM(
CASE
WHEN DATEDIFF(:toDate, DATE(i.bill_date_time)) BETWEEN 8 AND 30
THEN i.due_paise ELSE 0
END
), 0) AS days_8_to_30_paise,

COALESCE(SUM(
CASE
WHEN DATEDIFF(:toDate, DATE(i.bill_date_time)) BETWEEN 31 AND 60
THEN i.due_paise ELSE 0
END
), 0) AS days_31_to_60_paise,

COALESCE(SUM(
CASE
WHEN DATEDIFF(:toDate, DATE(i.bill_date_time)) > 60
THEN i.due_paise ELSE 0
END
), 0) AS days_60_plus_paise,

COUNT(i.id) AS invoice_count

FROM hospital_invoices i
WHERE i.tenant_id = :tenantId
AND i.hospital_id = :hospitalId
AND i.branch_id = :branchId
AND DATE(i.bill_date_time) BETWEEN :fromDate AND :toDate
AND i.is_deleted = 0
AND i.due_paise > 0
AND i.payer_type IS NOT NULL
AND i.payer_type <> ''
""");

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId)
.addValue("fromDate", fromDate)
.addValue("toDate", toDate);

if (department != null && !department.isBlank()) {
sql.append(" AND i.department = :department ");
params.addValue("department", department);
}

if (payerType != null && !payerType.isBlank()) {
sql.append(" AND i.payer_type = :payerType ");
params.addValue("payerType", payerType);
}

if (status != null && !status.isBlank()) {
sql.append(" AND i.status = :status ");
params.addValue("status", status);
}

if (paymentMethod != null && !paymentMethod.isBlank()) {
sql.append(" AND i.payment_method = :paymentMethod ");
params.addValue("paymentMethod", paymentMethod);
}

if (cashierId != null) {
sql.append(" AND i.cashier_id = :cashierId ");
params.addValue("cashierId", cashierId);
}

sql.append("""
GROUP BY i.payer_type
ORDER BY
(current_paise
+ days_1_to_7_paise
+ days_8_to_30_paise
+ days_31_to_60_paise
+ days_60_plus_paise) DESC
""");

return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
DueAgingResponse response = new DueAgingResponse();

response.setPayerType(rs.getString("payer_type"));
response.setCurrentPaise(rs.getLong("current_paise"));
response.setDays1To7Paise(rs.getLong("days_1_to_7_paise"));
response.setDays8To30Paise(rs.getLong("days_8_to_30_paise"));
response.setDays31To60Paise(rs.getLong("days_31_to_60_paise"));
response.setDays60PlusPaise(rs.getLong("days_60_plus_paise"));
response.setInvoiceCount(rs.getLong("invoice_count"));

return response;
});
}
public List<SettlementExceptionResponse> findSettlementExceptions(Long tenantId,
        Long hospitalId,
        String branchId,
        LocalDate fromDate,
        LocalDate toDate,
        String paymentMethod,
        String severity,
        String status,
        String owner) {

StringBuilder sql = new StringBuilder("""
SELECT
e.id,
e.invoice_no,
e.payment_method,
e.expected_paise,
e.settled_paise,
e.difference_paise,
e.reason,
e.severity,
e.owner,
e.due_at,
e.status
FROM hospital_settlement_exceptions e
WHERE e.tenant_id = :tenantId
AND e.hospital_id = :hospitalId
AND e.branch_id = :branchId
AND DATE(e.created_at) BETWEEN :fromDate AND :toDate
AND e.is_deleted = 0
""");

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId)
.addValue("fromDate", fromDate)
.addValue("toDate", toDate);

if (paymentMethod != null && !paymentMethod.isBlank()) {
sql.append(" AND e.payment_method = :paymentMethod ");
params.addValue("paymentMethod", paymentMethod);
}

if (severity != null && !severity.isBlank()) {
sql.append(" AND e.severity = :severity ");
params.addValue("severity", severity);
}

if (status != null && !status.isBlank()) {
sql.append(" AND e.status = :status ");
params.addValue("status", status);
}

if (owner != null && !owner.isBlank()) {
sql.append(" AND e.owner = :owner ");
params.addValue("owner", owner);
}

sql.append("""
ORDER BY
CASE
WHEN e.severity = 'Critical' THEN 1
WHEN e.severity = 'High' THEN 2
WHEN e.severity = 'Medium' THEN 3
WHEN e.severity = 'Low' THEN 4
ELSE 5
END,
e.due_at ASC,
e.created_at DESC
""");

return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
SettlementExceptionResponse response = new SettlementExceptionResponse();

response.setId(rs.getLong("id"));
response.setInvoiceNo(rs.getString("invoice_no"));
response.setPaymentMethod(rs.getString("payment_method"));
response.setExpectedPaise(rs.getLong("expected_paise"));
response.setSettledPaise(rs.getLong("settled_paise"));
response.setDifferencePaise(rs.getLong("difference_paise"));
response.setReason(rs.getString("reason"));
response.setSeverity(rs.getString("severity"));
response.setOwner(rs.getString("owner"));

if (rs.getTimestamp("due_at") != null) {
response.setDueAt(rs.getTimestamp("due_at").toLocalDateTime());
}

response.setStatus(rs.getString("status"));

return response;
});
}
public ReconciliationRunResponse createReconciliationRun(Long tenantId,
        Long hospitalId,
        String branchId,
        Long userId,
        ReconciliationRunRequest request,
        String channelsJson) {

String runCode = generateReconciliationRunCode();

String sql = """
INSERT INTO hospital_reconciliation_runs (
tenant_id,
hospital_id,
branch_id,
run_code,
from_date,
to_date,
channels_json,
notes,
status,
queued_at,
created_by
) VALUES (
:tenantId,
:hospitalId,
:branchId,
:runCode,
:fromDate,
:toDate,
CAST(:channelsJson AS JSON),
:notes,
:status,
:queuedAt,
:createdBy
)
""";

LocalDateTime queuedAt = LocalDateTime.now();

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId)
.addValue("runCode", runCode)
.addValue("fromDate", request.getFromDate())
.addValue("toDate", request.getToDate())
.addValue("channelsJson", channelsJson)
.addValue("notes", request.getNotes())
.addValue("status", "Queued")
.addValue("queuedAt", Timestamp.valueOf(queuedAt))
.addValue("createdBy", userId);

KeyHolder keyHolder = new GeneratedKeyHolder();

jdbc.update(sql, params, keyHolder, new String[]{"id"});

Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();

ReconciliationRunResponse response = new ReconciliationRunResponse();
response.setId(id);
response.setRunCode(runCode);
response.setStatus("Queued");
response.setQueuedAt(queuedAt);

return response;
}

private String generateReconciliationRunCode() {
return "REC-" + System.currentTimeMillis();
}
public ReconciliationRunResponse findReconciliationRunById(Long tenantId,
        Long hospitalId,
        String branchId,
        Long runId) {

String sql = """
SELECT
id,
run_code,
from_date,
to_date,
channels_json,
notes,
status,
queued_at,
started_at,
completed_at,
created_by,
created_at
FROM hospital_reconciliation_runs
WHERE id = :runId
AND tenant_id = :tenantId
AND hospital_id = :hospitalId
AND branch_id = :branchId
AND is_deleted = 0
""";

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("runId", runId)
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId);

List<ReconciliationRunResponse> rows = jdbc.query(sql, params, (rs, rowNum) -> {
ReconciliationRunResponse response = new ReconciliationRunResponse();

response.setId(rs.getLong("id"));
response.setRunCode(rs.getString("run_code"));

Date fromDate = rs.getDate("from_date");
if (fromDate != null) {
response.setFromDate(fromDate.toLocalDate());
}

Date toDate = rs.getDate("to_date");
if (toDate != null) {
response.setToDate(toDate.toLocalDate());
}

response.setChannels(parseChannelsJson(rs.getString("channels_json")));
response.setNotes(rs.getString("notes"));
response.setStatus(rs.getString("status"));

Timestamp queuedAt = rs.getTimestamp("queued_at");
if (queuedAt != null) {
response.setQueuedAt(queuedAt.toLocalDateTime());
}

Timestamp startedAt = rs.getTimestamp("started_at");
if (startedAt != null) {
response.setStartedAt(startedAt.toLocalDateTime());
}

Timestamp completedAt = rs.getTimestamp("completed_at");
if (completedAt != null) {
response.setCompletedAt(completedAt.toLocalDateTime());
}

long createdBy = rs.getLong("created_by");
response.setCreatedBy(rs.wasNull() ? null : createdBy);

Timestamp createdAt = rs.getTimestamp("created_at");
if (createdAt != null) {
response.setCreatedAt(createdAt.toLocalDateTime());
}

return response;
});

if (rows.isEmpty()) {
throw ApiException.notFound("Reconciliation run not found.");
}

return rows.get(0);
}

private List<String> parseChannelsJson(String channelsJson) {
if (channelsJson == null || channelsJson.isBlank()) {
return Collections.emptyList();
}

String cleaned = channelsJson
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
public ExportJobResponse createExportJob(Long tenantId,
        Long hospitalId,
        String branchId,
        Long userId,
        ExportJobRequest request,
        String filtersJson) {

String exportCode = generateExportCode();

String sql = """
INSERT INTO hospital_export_jobs (
tenant_id,
hospital_id,
branch_id,
export_code,
report_type,
export_format,
from_date,
to_date,
filters_json,
status,
expires_at,
created_by
) VALUES (
:tenantId,
:hospitalId,
:branchId,
:exportCode,
:reportType,
:exportFormat,
:fromDate,
:toDate,
CAST(:filtersJson AS JSON),
:status,
:expiresAt,
:createdBy
)
""";

LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId)
.addValue("exportCode", exportCode)
.addValue("reportType", request.getReportType())
.addValue("exportFormat", request.getFormat())
.addValue("fromDate", request.getFromDate())
.addValue("toDate", request.getToDate())
.addValue("filtersJson", filtersJson)
.addValue("status", "Queued")
.addValue("expiresAt", Timestamp.valueOf(expiresAt))
.addValue("createdBy", userId);

KeyHolder keyHolder = new GeneratedKeyHolder();

jdbc.update(sql, params, keyHolder, new String[]{"id"});

Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();

ExportJobResponse response = new ExportJobResponse();
response.setId(id);
response.setExportCode(exportCode);
response.setReportType(request.getReportType());
response.setFormat(request.getFormat());
response.setFromDate(request.getFromDate());
response.setToDate(request.getToDate());
response.setFilters(request.getFilters());
response.setStatus("Queued");
response.setExpiresAt(expiresAt);
response.setCreatedBy(userId);
response.setCreatedAt(LocalDateTime.now());

return response;
}

private String generateExportCode() {
return "EXP-" + System.currentTimeMillis();
}
public ExportJobResponse findExportJobById(Long tenantId,
        Long hospitalId,
        String branchId,
        Long exportId) {

String sql = """
SELECT
id,
export_code,
report_type,
export_format,
from_date,
to_date,
filters_json,
status,
file_url,
signed_download_url,
expires_at,
created_by,
created_at
FROM hospital_export_jobs
WHERE id = :exportId
AND tenant_id = :tenantId
AND hospital_id = :hospitalId
AND branch_id = :branchId
AND is_deleted = 0
""";

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("exportId", exportId)
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId);

List<ExportJobResponse> rows = jdbc.query(sql, params, (rs, rowNum) -> {
ExportJobResponse response = new ExportJobResponse();

response.setId(rs.getLong("id"));
response.setExportCode(rs.getString("export_code"));
response.setReportType(rs.getString("report_type"));
response.setFormat(rs.getString("export_format"));

Date fromDate = rs.getDate("from_date");
if (fromDate != null) {
response.setFromDate(fromDate.toLocalDate());
}

Date toDate = rs.getDate("to_date");
if (toDate != null) {
response.setToDate(toDate.toLocalDate());
}

response.setFilters(parseSimpleJsonObject(rs.getString("filters_json")));
response.setStatus(rs.getString("status"));
response.setFileUrl(rs.getString("file_url"));
response.setSignedDownloadUrl(rs.getString("signed_download_url"));

Timestamp expiresAt = rs.getTimestamp("expires_at");
if (expiresAt != null) {
response.setExpiresAt(expiresAt.toLocalDateTime());
}

long createdBy = rs.getLong("created_by");
response.setCreatedBy(rs.wasNull() ? null : createdBy);

Timestamp createdAt = rs.getTimestamp("created_at");
if (createdAt != null) {
response.setCreatedAt(createdAt.toLocalDateTime());
}

return response;
});

if (rows.isEmpty()) {
throw ApiException.notFound("Export job not found.");
}

return rows.get(0);
}

private Map<String, Object> parseSimpleJsonObject(String json) {
if (json == null || json.isBlank()) {
return Collections.emptyMap();
}

String cleaned = json.trim();

if (cleaned.equals("{}")) {
return Collections.emptyMap();
}

cleaned = cleaned
.replace("{", "")
.replace("}", "")
.replace("\"", "")
.trim();

if (cleaned.isBlank()) {
return Collections.emptyMap();
}

Map<String, Object> result = new HashMap<>();

String[] pairs = cleaned.split(",");

for (String pair : pairs) {
String[] keyValue = pair.split(":", 2);

if (keyValue.length == 2) {
result.put(keyValue[0].trim(), keyValue[1].trim());
}
}

return result;
}
public List<PaymentAuditResponse> findPaymentAudit(Long tenantId,
        Long hospitalId,
        String branchId,
        LocalDate fromDate,
        LocalDate toDate,
        String entityType,
        String action,
        Long actorUserId,
        Integer page,
        Integer limit) {

StringBuilder sql = new StringBuilder("""
SELECT
id,
entity_type,
entity_id,
action,
actor_user_id,
actor_role,
reason,
request_id,
ip_address,
user_agent,
created_at
FROM hospital_audit_events
WHERE tenant_id = :tenantId
AND hospital_id = :hospitalId
AND branch_id = :branchId
AND DATE(created_at) BETWEEN :fromDate AND :toDate
""");

MapSqlParameterSource params = auditParams(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
entityType,
action,
actorUserId
);

appendAuditFilters(sql, entityType, action, actorUserId);

sql.append("""
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset
""");

params.addValue("limit", limit);
params.addValue("offset", (page - 1) * limit);

return jdbc.query(sql.toString(), params, (rs, rowNum) -> {
PaymentAuditResponse response = new PaymentAuditResponse();

response.setId(rs.getLong("id"));
response.setEntityType(rs.getString("entity_type"));
response.setEntityId(rs.getString("entity_id"));
response.setAction(rs.getString("action"));

long actorId = rs.getLong("actor_user_id");
response.setActorUserId(rs.wasNull() ? null : actorId);

response.setActorRole(rs.getString("actor_role"));
response.setReason(rs.getString("reason"));
response.setRequestId(rs.getString("request_id"));
response.setIpAddress(rs.getString("ip_address"));
response.setUserAgent(rs.getString("user_agent"));

if (rs.getTimestamp("created_at") != null) {
response.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
}

return response;
});
}

public Long countPaymentAudit(Long tenantId,
Long hospitalId,
String branchId,
LocalDate fromDate,
LocalDate toDate,
String entityType,
String action,
Long actorUserId) {

StringBuilder sql = new StringBuilder("""
SELECT COUNT(*)
FROM hospital_audit_events
WHERE tenant_id = :tenantId
AND hospital_id = :hospitalId
AND branch_id = :branchId
AND DATE(created_at) BETWEEN :fromDate AND :toDate
""");

MapSqlParameterSource params = auditParams(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
entityType,
action,
actorUserId
);

appendAuditFilters(sql, entityType, action, actorUserId);

Long total = jdbc.queryForObject(sql.toString(), params, Long.class);
return total == null ? 0L : total;
}

private MapSqlParameterSource auditParams(Long tenantId,
Long hospitalId,
String branchId,
LocalDate fromDate,
LocalDate toDate,
String entityType,
String action,
Long actorUserId) {

MapSqlParameterSource params = new MapSqlParameterSource()
.addValue("tenantId", tenantId)
.addValue("hospitalId", hospitalId)
.addValue("branchId", branchId)
.addValue("fromDate", fromDate)
.addValue("toDate", toDate);

if (entityType != null && !entityType.isBlank()) {
params.addValue("entityType", entityType);
}

if (action != null && !action.isBlank()) {
params.addValue("action", action);
}

if (actorUserId != null) {
params.addValue("actorUserId", actorUserId);
}

return params;
}

private void appendAuditFilters(StringBuilder sql,
String entityType,
String action,
Long actorUserId) {

if (entityType != null && !entityType.isBlank()) {
sql.append(" AND entity_type = :entityType ");
}

if (action != null && !action.isBlank()) {
sql.append(" AND action = :action ");
}

if (actorUserId != null) {
sql.append(" AND actor_user_id = :actorUserId ");
}
}
}