package com.plasmit.pharmacy.sales.hospital.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.PaymentRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PharmacySalesRepository {

    private static final Logger log = LoggerFactory.getLogger(PharmacySalesRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PharmacySalesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateNextSaleNumber(Long hospitalId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pharmacy_sales WHERE hospital_id = ?",
                Integer.class,
                hospitalId
        );

        int next = count == null ? 1 : count + 1;

        return String.format("PH-SALE-%06d", next);
    }

    public String generateNextPaymentNumber(Long hospitalId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pharmacy_sale_payments WHERE hospital_id = ?",
                Integer.class,
                hospitalId
        );

        int next = count == null ? 1 : count + 1;

        return String.format("PH-PAY-%06d", next);
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

    public Optional<PrescriptionRelationRecord> findPrescriptionRelation(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long prescriptionId
    ) {
        String sql = """
                SELECT
                    pr.id AS prescription_id,
                    pr.patient_id,
                    pr.doctor_id,
                    pr.appointment_id
                FROM prescriptions pr
                WHERE pr.id = ?
                  AND pr.tenant_id = ?
                  AND pr.hospital_id = ?
                  AND (? IS NULL OR pr.branch_id = ?)
                  AND pr.status = 'ACTIVE'
                  AND pr.is_deleted = 0
                LIMIT 1
                """;

        List<PrescriptionRelationRecord> result = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new PrescriptionRelationRecord(
                        rs.getLong("prescription_id"),
                        rs.getLong("patient_id"),
                        rs.getLong("doctor_id"),
                        getNullableLong(rs, "appointment_id")
                ),
                prescriptionId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public List<PrescriptionMedicineRecord> findPrescriptionMedicines(
            Long tenantId,
            Long hospitalId,
            Long prescriptionId
    ) {
        String sql = """
                SELECT
                    id,
                    prescription_id,
                    medicine_name,
                    dosage,
                    frequency,
                    duration
                FROM prescription_medicines
                WHERE prescription_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND is_deleted = 0
                ORDER BY sort_order ASC, id ASC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new PrescriptionMedicineRecord(
                        rs.getLong("id"),
                        rs.getLong("prescription_id"),
                        rs.getString("medicine_name"),
                        rs.getString("dosage"),
                        rs.getString("frequency"),
                        rs.getString("duration")
                ),
                prescriptionId,
                tenantId,
                hospitalId
        );
    }

    public Optional<StockBatchRecord> findStockBatch(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long batchId
    ) {
        String sql = """
                SELECT
                    sb.id AS batch_id,
                    sb.tenant_id,
                    sb.hospital_id,
                    sb.branch_id,
                    sb.medicine_id,
                    m.medicine_code,
                    m.medicine_name,
                    sb.batch_number,
                    sb.current_quantity,
                    sb.sale_price,
                    sb.mrp,
                    m.tax_rate
                FROM pharmacy_stock_batches sb
                INNER JOIN medicines m
                    ON m.id = sb.medicine_id
                    AND m.tenant_id = sb.tenant_id
                    AND m.hospital_id = sb.hospital_id
                    AND m.status = 'ACTIVE'
                    AND m.is_deleted = 0
                WHERE sb.id = ?
                  AND sb.tenant_id = ?
                  AND sb.hospital_id = ?
                  AND (? IS NULL OR sb.branch_id = ?)
                  AND sb.status = 'ACTIVE'
                  AND sb.is_deleted = 0
                LIMIT 1
                """;

        List<StockBatchRecord> result = jdbcTemplate.query(
                sql,
                this::mapStockBatch,
                batchId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public Optional<StockBatchRecord> findAvailableBatchByMedicineName(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String medicineName
    ) {
        String sql = """
                SELECT
                    sb.id AS batch_id,
                    sb.tenant_id,
                    sb.hospital_id,
                    sb.branch_id,
                    sb.medicine_id,
                    m.medicine_code,
                    m.medicine_name,
                    sb.batch_number,
                    sb.current_quantity,
                    sb.sale_price,
                    sb.mrp,
                    m.tax_rate
                FROM pharmacy_stock_batches sb
                INNER JOIN medicines m
                    ON m.id = sb.medicine_id
                    AND m.tenant_id = sb.tenant_id
                    AND m.hospital_id = sb.hospital_id
                    AND m.status = 'ACTIVE'
                    AND m.is_deleted = 0
                WHERE sb.tenant_id = ?
                  AND sb.hospital_id = ?
                  AND (? IS NULL OR sb.branch_id = ?)
                  AND sb.current_quantity > 0
                  AND sb.status = 'ACTIVE'
                  AND sb.is_deleted = 0
                  AND (
                        LOWER(m.medicine_name) LIKE LOWER(?)
                        OR LOWER(m.generic_name) LIKE LOWER(?)
                        OR LOWER(m.brand_name) LIKE LOWER(?)
                  )
                ORDER BY sb.expiry_date ASC, sb.id ASC
                LIMIT 1
                """;

        String search = "%" + medicineName.trim() + "%";

        List<StockBatchRecord> result = jdbcTemplate.query(
                sql,
                this::mapStockBatch,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                search,
                search,
                search
        );

        return result.stream().findFirst();
    }

    public Long createSale(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            Long createdBy,
            String saleNumber,
            Long patientId,
            Long doctorId,
            Long appointmentId,
            Long prescriptionId,
            SaleTotals totals,
            String notes
    ) {
        log.debug("Creating pharmacy sale. tenantId={} hospitalId={} patientId={}",
                tenantId, hospitalId, patientId);

        String sql = """
                INSERT INTO pharmacy_sales (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    department_id,
                    sale_number,
                    patient_id,
                    doctor_id,
                    appointment_id,
                    prescription_id,
                    sale_date,
                    gross_amount,
                    discount_amount,
                    taxable_amount,
                    tax_amount,
                    net_amount,
                    paid_amount,
                    balance_amount,
                    payment_status,
                    sale_status,
                    notes,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURDATE(), ?, ?, ?, ?, ?, 0.00, ?, 'UNPAID', 'ACTIVE', ?, ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                departmentId,
                saleNumber,
                patientId,
                doctorId,
                appointmentId,
                prescriptionId,
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

    public void createSaleItem(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long saleId,
            SaleItemCalculated item
    ) {
        String sql = """
                INSERT INTO pharmacy_sale_items (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    sale_id,
                    medicine_id,
                    batch_id,
                    medicine_code,
                    medicine_name,
                    batch_number,
                    quantity,
                    unit_price,
                    mrp,
                    discount_amount,
                    taxable_amount,
                    tax_rate,
                    tax_amount,
                    line_total,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                saleId,
                item.medicineId(),
                item.batchId(),
                item.medicineCode(),
                item.medicineName(),
                item.batchNumber(),
                item.quantity(),
                item.unitPrice(),
                item.mrp(),
                item.discountAmount(),
                item.taxableAmount(),
                item.taxRate(),
                item.taxAmount(),
                item.lineTotal()
        );
    }

    public void reduceBatchStock(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long batchId,
            BigDecimal newQuantity,
            Long updatedBy
    ) {
        String sql = """
                UPDATE pharmacy_stock_batches
                SET current_quantity = ?,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        jdbcTemplate.update(sql, newQuantity, updatedBy, batchId, tenantId, hospitalId, branchId, branchId);
    }

    public void createStockMovement(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long medicineId,
            Long batchId,
            Long saleId,
            BigDecimal quantity,
            BigDecimal beforeQuantity,
            BigDecimal afterQuantity,
            Long createdBy
    ) {
        String sql = """
                INSERT INTO pharmacy_stock_movements (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    medicine_id,
                    batch_id,
                    movement_type,
                    reference_type,
                    reference_id,
                    quantity,
                    before_quantity,
                    after_quantity,
                    remarks,
                    created_by,
                    created_at,
                    is_deleted
                )
                VALUES (?, ?, ?, ?, ?, 'SALE', 'PHARMACY_SALE', ?, ?, ?, ?, 'Stock reduced from pharmacy sale.', ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                medicineId,
                batchId,
                saleId,
                quantity,
                beforeQuantity,
                afterQuantity,
                createdBy
        );
    }

    public Optional<SaleRecord> findSaleById(Long saleId, Long tenantId, Long hospitalId, Long branchId) {
        String sql = """
                SELECT
                    ps.id,
                    ps.tenant_id,
                    ps.hospital_id,
                    ps.branch_id,
                    ps.department_id,
                    ps.sale_number,
                    ps.patient_id,
                    p.full_name AS patient_name,
                    p.phone AS patient_phone,
                    ps.doctor_id,
                    d.full_name AS doctor_name,
                    ps.appointment_id,
                    ps.prescription_id,
                    ps.sale_date,
                    ps.gross_amount,
                    ps.discount_amount,
                    ps.taxable_amount,
                    ps.tax_amount,
                    ps.net_amount,
                    ps.paid_amount,
                    ps.balance_amount,
                    ps.payment_status,
                    ps.sale_status,
                    ps.notes,
                    ps.created_at,
                    ps.updated_at
                FROM pharmacy_sales ps
                INNER JOIN patients p
                    ON p.id = ps.patient_id
                    AND p.tenant_id = ps.tenant_id
                    AND p.hospital_id = ps.hospital_id
                LEFT JOIN doctors d
                    ON d.id = ps.doctor_id
                    AND d.tenant_id = ps.tenant_id
                    AND d.hospital_id = ps.hospital_id
                WHERE ps.id = ?
                  AND ps.tenant_id = ?
                  AND ps.hospital_id = ?
                  AND (? IS NULL OR ps.branch_id = ?)
                  AND ps.is_deleted = 0
                LIMIT 1
                """;

        List<SaleRecord> result = jdbcTemplate.query(
                sql,
                this::mapSale,
                saleId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public List<SaleRecord> findSales(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long patientId,
            Long prescriptionId,
            String paymentStatus,
            String saleStatus,
            int limit,
            int offset
    ) {
        String sql = """
                SELECT
                    ps.id,
                    ps.tenant_id,
                    ps.hospital_id,
                    ps.branch_id,
                    ps.department_id,
                    ps.sale_number,
                    ps.patient_id,
                    p.full_name AS patient_name,
                    p.phone AS patient_phone,
                    ps.doctor_id,
                    d.full_name AS doctor_name,
                    ps.appointment_id,
                    ps.prescription_id,
                    ps.sale_date,
                    ps.gross_amount,
                    ps.discount_amount,
                    ps.taxable_amount,
                    ps.tax_amount,
                    ps.net_amount,
                    ps.paid_amount,
                    ps.balance_amount,
                    ps.payment_status,
                    ps.sale_status,
                    ps.notes,
                    ps.created_at,
                    ps.updated_at
                FROM pharmacy_sales ps
                INNER JOIN patients p
                    ON p.id = ps.patient_id
                    AND p.tenant_id = ps.tenant_id
                    AND p.hospital_id = ps.hospital_id
                LEFT JOIN doctors d
                    ON d.id = ps.doctor_id
                    AND d.tenant_id = ps.tenant_id
                    AND d.hospital_id = ps.hospital_id
                WHERE ps.tenant_id = ?
                  AND ps.hospital_id = ?
                  AND (? IS NULL OR ps.branch_id = ?)
                  AND (? IS NULL OR ps.patient_id = ?)
                  AND (? IS NULL OR ps.prescription_id = ?)
                  AND (? IS NULL OR ps.payment_status = ?)
                  AND (? IS NULL OR ps.sale_status = ?)
                  AND ps.is_deleted = 0
                ORDER BY ps.created_at DESC, ps.id DESC
                LIMIT ? OFFSET ?
                """;

        String paymentStatusValue = paymentStatus == null || paymentStatus.isBlank() ? null : paymentStatus.trim();
        String saleStatusValue = saleStatus == null || saleStatus.isBlank() ? null : saleStatus.trim();

        return jdbcTemplate.query(
                sql,
                this::mapSale,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                patientId,
                patientId,
                prescriptionId,
                prescriptionId,
                paymentStatusValue,
                paymentStatusValue,
                saleStatusValue,
                saleStatusValue,
                limit,
                offset
        );
    }

    public List<SaleItemRecord> findSaleItems(Long saleId, Long tenantId, Long hospitalId) {
        String sql = """
                SELECT
                    id,
                    sale_id,
                    medicine_id,
                    batch_id,
                    medicine_code,
                    medicine_name,
                    batch_number,
                    quantity,
                    unit_price,
                    mrp,
                    discount_amount,
                    taxable_amount,
                    tax_rate,
                    tax_amount,
                    line_total
                FROM pharmacy_sale_items
                WHERE sale_id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND is_deleted = 0
                ORDER BY id ASC
                """;

        return jdbcTemplate.query(sql, this::mapSaleItem, saleId, tenantId, hospitalId);
    }

    public Long createPayment(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long saleId,
            Long patientId,
            String paymentNumber,
            PaymentRequest request,
            Long createdBy
    ) {
        String sql = """
                INSERT INTO pharmacy_sale_payments (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    sale_id,
                    patient_id,
                    payment_number,
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
                saleId,
                patientId,
                paymentNumber,
                request.paymentMethod() == null || request.paymentMethod().isBlank() ? "CASH" : request.paymentMethod(),
                request.amount(),
                request.transactionReference(),
                request.remarks(),
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateSalePaymentTotals(
            Long saleId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            BigDecimal paidAmount,
            BigDecimal balanceAmount,
            String paymentStatus,
            Long updatedBy
    ) {
        String sql = """
                UPDATE pharmacy_sales
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
                saleId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public int cancelSale(
            Long saleId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            String remarks
    ) {
        String sql = """
                UPDATE pharmacy_sales
                SET sale_status = 'CANCELLED',
                    notes = CONCAT(COALESCE(notes, ''), '\nCancellation: ', ?),
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND sale_status <> 'CANCELLED'
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, remarks, updatedBy, saleId, tenantId, hospitalId, branchId, branchId);
    }

    public void saveStatusHistory(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long saleId,
            String oldStatus,
            String newStatus,
            String remarks,
            Long changedBy
    ) {
        String sql = """
                INSERT INTO pharmacy_sale_status_history (
                    tenant_id,
                    hospital_id,
                    branch_id,
                    sale_id,
                    old_status,
                    new_status,
                    remarks,
                    changed_by,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """;

        jdbcTemplate.update(sql, tenantId, hospitalId, branchId, saleId, oldStatus, newStatus, remarks, changedBy);
    }

    private StockBatchRecord mapStockBatch(ResultSet rs, int rowNum) throws SQLException {
        return new StockBatchRecord(
                rs.getLong("batch_id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                rs.getLong("medicine_id"),
                rs.getString("medicine_code"),
                rs.getString("medicine_name"),
                rs.getString("batch_number"),
                rs.getBigDecimal("current_quantity"),
                rs.getBigDecimal("sale_price"),
                rs.getBigDecimal("mrp"),
                rs.getBigDecimal("tax_rate")
        );
    }

    private SaleRecord mapSale(ResultSet rs, int rowNum) throws SQLException {
        return new SaleRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                getNullableLong(rs, "department_id"),
                rs.getString("sale_number"),
                rs.getLong("patient_id"),
                rs.getString("patient_name"),
                rs.getString("patient_phone"),
                getNullableLong(rs, "doctor_id"),
                rs.getString("doctor_name"),
                getNullableLong(rs, "appointment_id"),
                getNullableLong(rs, "prescription_id"),
                rs.getDate("sale_date").toLocalDate(),
                rs.getBigDecimal("gross_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("taxable_amount"),
                rs.getBigDecimal("tax_amount"),
                rs.getBigDecimal("net_amount"),
                rs.getBigDecimal("paid_amount"),
                rs.getBigDecimal("balance_amount"),
                rs.getString("payment_status"),
                rs.getString("sale_status"),
                rs.getString("notes"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private SaleItemRecord mapSaleItem(ResultSet rs, int rowNum) throws SQLException {
        return new SaleItemRecord(
                rs.getLong("id"),
                rs.getLong("sale_id"),
                rs.getLong("medicine_id"),
                rs.getLong("batch_id"),
                rs.getString("medicine_code"),
                rs.getString("medicine_name"),
                rs.getString("batch_number"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("unit_price"),
                rs.getBigDecimal("mrp"),
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

    public record PrescriptionRelationRecord(
            Long prescriptionId,
            Long patientId,
            Long doctorId,
            Long appointmentId
    ) {
    }

    public record PrescriptionMedicineRecord(
            Long id,
            Long prescriptionId,
            String medicineName,
            String dosage,
            String frequency,
            String duration
    ) {
    }

    public record StockBatchRecord(
            Long batchId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long medicineId,
            String medicineCode,
            String medicineName,
            String batchNumber,
            BigDecimal currentQuantity,
            BigDecimal salePrice,
            BigDecimal mrp,
            BigDecimal taxRate
    ) {
    }

    public record SaleItemCalculated(
            Long medicineId,
            Long batchId,
            String medicineCode,
            String medicineName,
            String batchNumber,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal mrp,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal lineTotal,
            BigDecimal beforeStock,
            BigDecimal afterStock
    ) {
    }

    public record SaleTotals(
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal netAmount
    ) {
    }

    public record SaleRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            String saleNumber,
            Long patientId,
            String patientName,
            String patientPhone,
            Long doctorId,
            String doctorName,
            Long appointmentId,
            Long prescriptionId,
            LocalDate saleDate,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal netAmount,
            BigDecimal paidAmount,
            BigDecimal balanceAmount,
            String paymentStatus,
            String saleStatus,
            String notes,
            String createdAt,
            String updatedAt
    ) {
    }

    public record SaleItemRecord(
            Long id,
            Long saleId,
            Long medicineId,
            Long batchId,
            String medicineCode,
            String medicineName,
            String batchNumber,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal mrp,
            BigDecimal discountAmount,
            BigDecimal taxableAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal lineTotal
    ) {
    }
}