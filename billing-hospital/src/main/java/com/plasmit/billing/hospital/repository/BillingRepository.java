package com.plasmit.billing.hospital.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BillingRepository {

    private static final Logger log = LoggerFactory.getLogger(BillingRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public BillingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateNextInvoiceNumber(Long hospitalId) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM billing_invoices WHERE hospital_id = ?",
                Integer.class,
                hospitalId
        );

        int next = count == null ? 1 : count + 1;

        return String.format("INV-%06d", next);
    }

    public String generateNextPaymentNumber(Long hospitalId) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM billing_payments WHERE hospital_id = ?",
                Integer.class,
                hospitalId
        );

        int next = count == null ? 1 : count + 1;

        return String.format("PAY-%06d", next);
    }

    public boolean patientExists(Long tenantId, Long hospitalId, Long patientId) {

        String sql = """
                SELECT COUNT(*)
                FROM patients
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, patientId, tenantId, hospitalId);
        return count != null && count > 0;
    }

    public boolean appointmentExistsForPatient(Long tenantId, Long hospitalId, Long patientId, Long appointmentId) {

        if (appointmentId == null) {
            return true;
        }

        String sql = """
                SELECT COUNT(*)
                FROM appointments
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND patient_id = ?
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, appointmentId, tenantId, hospitalId, patientId);
        return count != null && count > 0;
    }

    public Optional<BillingServiceRecord> findService(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String serviceType,
            Long referenceId
    ) {

        String sql;

        if ("DOCTOR_CONSULTATION".equals(serviceType)) {
            sql = """
                    SELECT
                        'DOCTOR_CONSULTATION' AS service_type,
                        id AS reference_id,
                        doctor_code AS service_code,
                        CONCAT(full_name, ' - Consultation') AS service_name,
                        specialization AS category,
                        consultation_fee AS amount,
                        0.00 AS tax_rate
                    FROM doctors
                    WHERE id = ?
                      AND tenant_id = ?
                      AND hospital_id = ?
                      AND (? IS NULL OR branch_id = ?)
                      AND status = 'ACTIVE'
                      AND is_deleted = 0
                    LIMIT 1
                    """;
        } else if ("LAB_TEST".equals(serviceType)) {
            sql = """
                    SELECT
                        'LAB_TEST' AS service_type,
                        id AS reference_id,
                        test_code AS service_code,
                        test_name AS service_name,
                        test_category AS category,
                        price AS amount,
                        tax_rate AS tax_rate
                    FROM lab_tests
                    WHERE id = ?
                      AND tenant_id = ?
                      AND hospital_id = ?
                      AND (? IS NULL OR branch_id = ?)
                      AND status = 'ACTIVE'
                      AND is_deleted = 0
                    LIMIT 1
                    """;
        } else if ("LAB_PACKAGE".equals(serviceType)) {
            sql = """
                    SELECT
                        'LAB_PACKAGE' AS service_type,
                        id AS reference_id,
                        package_code AS service_code,
                        package_name AS service_name,
                        package_category AS category,
                        package_price AS amount,
                        tax_rate AS tax_rate
                    FROM lab_test_packages
                    WHERE id = ?
                      AND tenant_id = ?
                      AND hospital_id = ?
                      AND (? IS NULL OR branch_id = ?)
                      AND status = 'ACTIVE'
                      AND is_deleted = 0
                    LIMIT 1
                    """;
        } else {
            return Optional.empty();
        }

        List<BillingServiceRecord> result = jdbcTemplate.query(
                sql,
                this::mapBillingService,
                referenceId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public List<BillingServiceRecord> findBillingServices(Long tenantId, Long hospitalId, Long branchId, String query) {

        String searchValue = query == null || query.isBlank() ? null : "%" + query.trim() + "%";

        String sql = """
                SELECT
                    'DOCTOR_CONSULTATION' AS service_type,
                    id AS reference_id,
                    doctor_code AS service_code,
                    CONCAT(full_name, ' - Consultation') AS service_name,
                    specialization AS category,
                    consultation_fee AS amount,
                    0.00 AS tax_rate
                FROM doctors
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                  AND (? IS NULL OR full_name LIKE ? OR doctor_code LIKE ? OR specialization LIKE ?)

                UNION ALL

                SELECT
                    'LAB_TEST' AS service_type,
                    id AS reference_id,
                    test_code AS service_code,
                    test_name AS service_name,
                    test_category AS category,
                    price AS amount,
                    tax_rate AS tax_rate
                FROM lab_tests
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_billable = 1
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                  AND (? IS NULL OR test_name LIKE ? OR test_code LIKE ? OR test_category LIKE ?)

                UNION ALL

                SELECT
                    'LAB_PACKAGE' AS service_type,
                    id AS reference_id,
                    package_code AS service_code,
                    package_name AS service_name,
                    package_category AS category,
                    package_price AS amount,
                    tax_rate AS tax_rate
                FROM lab_test_packages
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_billable = 1
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                  AND (? IS NULL OR package_name LIKE ? OR package_code LIKE ? OR package_category LIKE ?)
                """;

        return jdbcTemplate.query(
                sql,
                this::mapBillingService,

                tenantId,
                hospitalId,
                branchId,
                branchId,
                searchValue,
                searchValue,
                searchValue,
                searchValue,

                tenantId,
                hospitalId,
                branchId,
                branchId,
                searchValue,
                searchValue,
                searchValue,
                searchValue,

                tenantId,
                hospitalId,
                branchId,
                branchId,
                searchValue,
                searchValue,
                searchValue,
                searchValue
        );
    }

    public Long createInvoice(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            Long createdBy,
            String invoiceNumber,
            Long patientId,
            Long appointmentId,
            InvoiceTotals totals,
            String notes
    ) {

        log.debug("Creating billing invoice. tenantId={} hospitalId={} branchId={} patientId={}",
                tenantId, hospitalId, branchId, patientId);

        String sql = """
                INSERT INTO billing_invoices (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    department_id,
                    invoice_number,
                    patient_id,
                    appointment_id,
                    invoice_date,
                    due_date,
                    gross_amount,
                    discount_amount,
                    taxable_amount,
                    tax_amount,
                    net_amount,
                    paid_amount,
                    balance_amount,
                    payment_status,
                    invoice_status,
                    notes,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, CURDATE(), CURDATE(), ?, ?, ?, ?, ?, 0.00, ?, 'UNPAID', 'ACTIVE', ?, ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                departmentId,
                invoiceNumber,
                patientId,
                appointmentId,
                totals.grossAmount(),
                totals.discountAmount(),
                totals.taxableAmount(),
                totals.taxAmount(),
                totals.netAmount(),
                totals.netAmount(),
                notes,
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void createInvoiceItem(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long invoiceId,
            InvoiceItemCalculated item
    ) {

        String sql = """
                INSERT INTO billing_invoice_items (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    invoice_id,
                    service_type,
                    reference_id,
                    service_code,
                    service_name,
                    category,
                    quantity,
                    unit_price,
                    discount_amount,
                    taxable_amount,
                    tax_rate,
                    tax_amount,
                    line_total,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                invoiceId,
                item.serviceType(),
                item.referenceId(),
                item.serviceCode(),
                item.serviceName(),
                item.category(),
                item.quantity(),
                item.unitPrice(),
                item.discountAmount(),
                item.taxableAmount(),
                item.taxRate(),
                item.taxAmount(),
                item.lineTotal()
        );
    }

    public List<InvoiceRecord> findInvoices(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String paymentStatus,
            String invoiceStatus,
            Long patientId,
            int limit,
            int offset
    ) {

        String sql = """
                SELECT
                    bi.id,
                    bi.tenant_id,
                    bi.hospital_id,
                    bi.branch_id,
                    bi.department_id,
                    bi.invoice_number,
                    bi.patient_id,
                    p.full_name AS patient_name,
                    p.phone AS patient_phone,
                    bi.appointment_id,
                    bi.invoice_date,
                    bi.due_date,
                    bi.gross_amount,
                    bi.discount_amount,
                    bi.taxable_amount,
                    bi.tax_amount,
                    bi.net_amount,
                    bi.paid_amount,
                    bi.balance_amount,
                    bi.payment_status,
                    bi.invoice_status,
                    bi.notes,
                    bi.created_at,
                    bi.updated_at
                FROM billing_invoices bi
                INNER JOIN patients p
                    ON p.id = bi.patient_id
                    AND p.tenant_id = bi.tenant_id
                    AND p.hospital_id = bi.hospital_id
                WHERE bi.tenant_id = ?
                  AND bi.hospital_id = ?
                  AND (? IS NULL OR bi.branch_id = ?)
                  AND (? IS NULL OR bi.payment_status = ?)
                  AND (? IS NULL OR bi.invoice_status = ?)
                  AND (? IS NULL OR bi.patient_id = ?)
                  AND bi.is_deleted = 0
                ORDER BY bi.created_at DESC, bi.id DESC
                LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                this::mapInvoice,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                emptyToNull(paymentStatus),
                emptyToNull(paymentStatus),
                emptyToNull(invoiceStatus),
                emptyToNull(invoiceStatus),
                patientId,
                patientId,
                limit,
                offset
        );
    }

    public Optional<InvoiceRecord> findInvoiceById(Long invoiceId, Long tenantId, Long hospitalId, Long branchId) {

        String sql = """
                SELECT
                    bi.id,
                    bi.tenant_id,
                    bi.hospital_id,
                    bi.branch_id,
                    bi.department_id,
                    bi.invoice_number,
                    bi.patient_id,
                    p.full_name AS patient_name,
                    p.phone AS patient_phone,
                    bi.appointment_id,
                    bi.invoice_date,
                    bi.due_date,
                    bi.gross_amount,
                    bi.discount_amount,
                    bi.taxable_amount,
                    bi.tax_amount,
                    bi.net_amount,
                    bi.paid_amount,
                    bi.balance_amount,
                    bi.payment_status,
                    bi.invoice_status,
                    bi.notes,
                    bi.created_at,
                    bi.updated_at
                FROM billing_invoices bi
                INNER JOIN patients p
                    ON p.id = bi.patient_id
                    AND p.tenant_id = bi.tenant_id
                    AND p.hospital_id = bi.hospital_id
                WHERE bi.id = ?
                  AND bi.tenant_id = ?
                  AND bi.hospital_id = ?
                  AND (? IS NULL OR bi.branch_id = ?)
                  AND bi.is_deleted = 0
                LIMIT 1
                """;

        List<InvoiceRecord> result = jdbcTemplate.query(
                sql,
                this::mapInvoice,
                invoiceId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public List<InvoiceItemRecord> findInvoiceItems(Long invoiceId, Long tenantId, Long hospitalId) {

        String sql = """
                SELECT
                    id,
                    invoice_id,
                    service_type,
                    reference_id,
                    service_code,
                    service_name,
                    category,
                    quantity,
                    unit_price,
                    discount_amount,
                    taxable_amount,
                    tax_rate,
                    tax_amount,
                    line_total
                FROM billing_invoice_items
                WHERE invoice_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND is_deleted = 0
                ORDER BY id ASC
                """;

        return jdbcTemplate.query(sql, this::mapInvoiceItem, invoiceId, tenantId, hospitalId);
    }

    public Long createPayment(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long createdBy,
            String paymentNumber,
            Long patientId,
            Long invoiceId,
            BigDecimal amount,
            String paymentMethod,
            String transactionReference,
            String remarks
    ) {

        String sql = """
                INSERT INTO billing_payments (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    payment_number,
                    patient_id,
                    invoice_id,
                    payment_date,
                    payment_method,
                    amount,
                    transaction_reference,
                    remarks,
                    payment_status,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?, 'SUCCESS', ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                paymentNumber,
                patientId,
                invoiceId,
                paymentMethod,
                amount,
                transactionReference,
                remarks,
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void createPaymentAllocation(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long paymentId,
            Long invoiceId,
            BigDecimal amount
    ) {

        String sql = """
                INSERT INTO billing_payment_allocations (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    payment_id,
                    invoice_id,
                    allocated_amount,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, NOW(), 0)
                """;

        jdbcTemplate.update(sql, tenantId, hospitalId, branchId, paymentId, invoiceId, amount);
    }

    public void updateInvoicePaymentTotals(
            Long invoiceId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            BigDecimal paidAmount,
            BigDecimal balanceAmount,
            String paymentStatus,
            Long updatedBy
    ) {

        String sql = """
                UPDATE billing_invoices
                SET paid_amount = ?,
                    balance_amount = ?,
                    payment_status = ?,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        jdbcTemplate.update(
                sql,
                paidAmount,
                balanceAmount,
                paymentStatus,
                updatedBy,
                invoiceId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public int cancelInvoice(
            Long invoiceId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            String remarks
    ) {

        String sql = """
                UPDATE billing_invoices
                SET invoice_status = 'CANCELLED',
                    notes = CONCAT(COALESCE(notes, ''), '\nCancellation: ', ?),
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND invoice_status <> 'CANCELLED'
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, remarks, updatedBy, invoiceId, tenantId, hospitalId, branchId, branchId);
    }

    public void saveStatusHistory(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long invoiceId,
            String oldStatus,
            String newStatus,
            String remarks,
            Long changedBy
    ) {

        String sql = """
                INSERT INTO billing_status_history (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    invoice_id,
                    old_status,
                    new_status,
                    remarks,
                    changed_by,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """;

        jdbcTemplate.update(sql, tenantId, hospitalId, branchId, invoiceId, oldStatus, newStatus, remarks, changedBy);
    }

    public DashboardBootstrap bootstrap(Long tenantId, Long hospitalId, Long branchId) {

        String sql = """
                SELECT
                    COUNT(*) AS invoice_count,
                    COALESCE(SUM(net_amount), 0) AS total_billed,
                    COALESCE(SUM(paid_amount), 0) AS total_paid,
                    COALESCE(SUM(balance_amount), 0) AS total_due
                FROM billing_invoices
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND invoice_status = 'ACTIVE'
                  AND is_deleted = 0
                """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new DashboardBootstrap(
                        rs.getInt("invoice_count"),
                        rs.getBigDecimal("total_billed"),
                        rs.getBigDecimal("total_paid"),
                        rs.getBigDecimal("total_due")
                ),
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    private BillingServiceRecord mapBillingService(ResultSet rs, int rowNum) throws SQLException {
        return new BillingServiceRecord(
                rs.getString("service_type"),
                rs.getLong("reference_id"),
                rs.getString("service_code"),
                rs.getString("service_name"),
                rs.getString("category"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("tax_rate")
        );
    }

    private InvoiceRecord mapInvoice(ResultSet rs, int rowNum) throws SQLException {
        return new InvoiceRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                getNullableLong(rs, "department_id"),
                rs.getString("invoice_number"),
                rs.getLong("patient_id"),
                rs.getString("patient_name"),
                rs.getString("patient_phone"),
                getNullableLong(rs, "appointment_id"),
                rs.getDate("invoice_date").toLocalDate(),
                rs.getDate("due_date") == null ? null : rs.getDate("due_date").toLocalDate(),
                rs.getBigDecimal("gross_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("taxable_amount"),
                rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("net_amount"),
                rs.getBigDecimal("paid_amount"),
                rs.getBigDecimal("balance_amount"),
                rs.getString("payment_status"),
                rs.getString("invoice_status"),
                rs.getString("notes"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private InvoiceItemRecord mapInvoiceItem(ResultSet rs, int rowNum) throws SQLException {
        return new InvoiceItemRecord(
                rs.getLong("id"),
                rs.getLong("invoice_id"),
                rs.getString("service_type"),
                getNullableLong(rs, "reference_id"),
                rs.getString("service_code"),
                rs.getString("service_name"),
                rs.getString("category"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("taxable_amount"),
                rs.getBigDecimal("tax_rate"),
                rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("line_total")
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record BillingServiceRecord(
            String serviceType,
            Long referenceId,
            String serviceCode,
            String serviceName,
            String category,
            BigDecimal amount,
            BigDecimal taxRate
    ) {
    }

    public record InvoiceTotals(
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal netAmount
    ) {
    }

    public record InvoiceItemCalculated(
            String serviceType,
            Long referenceId,
            String serviceCode,
            String serviceName,
            String category,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal lineTotal
    ) {
    }

    public record InvoiceRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            String invoiceNumber,
            Long patientId,
            String patientName,
            String patientPhone,
            Long appointmentId,
            LocalDate invoiceDate,
            LocalDate dueDate,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal netAmount,
            BigDecimal paidAmount,
            BigDecimal balanceAmount,
            String paymentStatus,
            String invoiceStatus,
            String notes,
            String createdAt,
            String updatedAt
    ) {
    }

    public record InvoiceItemRecord(
            Long id,
            Long invoiceId,
            String serviceType,
            Long referenceId,
            String serviceCode,
            String serviceName,
            String category,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal lineTotal
    ) {
    }

    public record DashboardBootstrap(
            Integer invoiceCount,
            BigDecimal totalBilled,
            BigDecimal totalPaid,
            BigDecimal totalDue
    ) {
    }
}