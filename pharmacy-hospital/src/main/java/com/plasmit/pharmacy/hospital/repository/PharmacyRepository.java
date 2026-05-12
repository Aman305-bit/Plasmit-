package com.plasmit.pharmacy.hospital.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.plasmit.pharmacy.hospital.service.PharmacyService.AdjustStockRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.CreateCategoryRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.CreateMedicineRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.CreateStockBatchRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.UpdateMedicineRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PharmacyRepository {

    private static final Logger log = LoggerFactory.getLogger(PharmacyRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PharmacyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateNextCategoryCode(Long hospitalId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM medicine_categories WHERE hospital_id = ?",
                Integer.class,
                hospitalId
        );
        return String.format("MED-CAT-%03d", count == null ? 1 : count + 1);
    }

    public String generateNextMedicineCode(Long hospitalId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM medicines WHERE hospital_id = ?",
                Integer.class,
                hospitalId
        );
        return String.format("MED-%06d", count == null ? 1 : count + 1);
    }

    public Long createCategory(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long createdBy,
            String categoryCode,
            CreateCategoryRequest request
    ) {
        String sql = """
                INSERT INTO medicine_categories (
                    tenant_id, hospital_id, branch_id, category_code, category_name,
                    description, status, created_by, created_at, is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                categoryCode,
                request.categoryName(),
                request.description(),
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<CategoryRecord> findCategories(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String status
    ) {
        String statusValue = status == null || status.isBlank() ? null : status.trim();

        String sql = """
                SELECT id, tenant_id, hospital_id, branch_id, category_code, category_name,
                       description, status, created_at, updated_at
                FROM medicine_categories
                WHERE tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND (? IS NULL OR status = ?)
                  AND is_deleted = 0
                ORDER BY category_name ASC
                """;

        return jdbcTemplate.query(
                sql,
                this::mapCategory,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                statusValue,
                statusValue
        );
    }

    public boolean categoryExists(Long tenantId, Long hospitalId, Long categoryId) {
        if (categoryId == null) {
            return true;
        }

        String sql = """
                SELECT COUNT(*)
                FROM medicine_categories
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND status = 'ACTIVE'
                  AND is_deleted = 0
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, categoryId, tenantId, hospitalId);
        return count != null && count > 0;
    }

    public Long createMedicine(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long createdBy,
            String medicineCode,
            CreateMedicineRequest request
    ) {
        log.debug("Creating medicine. tenantId={} hospitalId={} branchId={}", tenantId, hospitalId, branchId);

        String sql = """
                INSERT INTO medicines (
                    tenant_id, hospital_id, branch_id, medicine_code, medicine_name,
                    category_id, generic_name, brand_name, manufacturer,
                    medicine_type, strength, unit, hsn_code, tax_rate,
                    purchase_price, sale_price, mrp, reorder_level,
                    prescription_required, is_billable, status,
                    created_by, created_at, is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 'ACTIVE', ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                medicineCode,
                request.medicineName(),
                request.categoryId(),
                request.genericName(),
                request.brandName(),
                request.manufacturer(),
                request.medicineType(),
                request.strength(),
                request.unit(),
                request.hsnCode(),
                defaultBigDecimal(request.taxRate()),
                defaultBigDecimal(request.purchasePrice()),
                defaultBigDecimal(request.salePrice()),
                defaultBigDecimal(request.mrp()),
                defaultBigDecimal(request.reorderLevel()),
                request.prescriptionRequired() == null || request.prescriptionRequired() ? 1 : 0,
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int updateMedicine(
            Long medicineId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long updatedBy,
            UpdateMedicineRequest request
    ) {
        String sql = """
                UPDATE medicines
                SET medicine_name = ?,
                    category_id = ?,
                    generic_name = ?,
                    brand_name = ?,
                    manufacturer = ?,
                    medicine_type = ?,
                    strength = ?,
                    unit = ?,
                    hsn_code = ?,
                    tax_rate = ?,
                    purchase_price = ?,
                    sale_price = ?,
                    mrp = ?,
                    reorder_level = ?,
                    prescription_required = ?,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(
                sql,
                request.medicineName(),
                request.categoryId(),
                request.genericName(),
                request.brandName(),
                request.manufacturer(),
                request.medicineType(),
                request.strength(),
                request.unit(),
                request.hsnCode(),
                defaultBigDecimal(request.taxRate()),
                defaultBigDecimal(request.purchasePrice()),
                defaultBigDecimal(request.salePrice()),
                defaultBigDecimal(request.mrp()),
                defaultBigDecimal(request.reorderLevel()),
                request.prescriptionRequired() == null || request.prescriptionRequired() ? 1 : 0,
                updatedBy,
                medicineId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );
    }

    public List<MedicineRecord> findMedicines(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String query,
            Long categoryId,
            String status,
            int limit,
            int offset
    ) {
        String searchValue = query == null || query.isBlank() ? null : "%" + query.trim() + "%";
        String statusValue = status == null || status.isBlank() ? null : status.trim();

        String sql = """
                SELECT
                    m.id, m.tenant_id, m.hospital_id, m.branch_id,
                    m.medicine_code, m.medicine_name, m.category_id,
                    c.category_name,
                    m.generic_name, m.brand_name, m.manufacturer,
                    m.medicine_type, m.strength, m.unit, m.hsn_code,
                    m.tax_rate, m.purchase_price, m.sale_price, m.mrp,
                    m.reorder_level, m.prescription_required, m.is_billable,
                    m.status, m.created_at, m.updated_at
                FROM medicines m
                LEFT JOIN medicine_categories c
                    ON c.id = m.category_id
                    AND c.tenant_id = m.tenant_id
                    AND c.hospital_id = m.hospital_id
                    AND c.is_deleted = 0
                WHERE m.tenant_id = ?
                  AND m.hospital_id = ?
                  AND (? IS NULL OR m.branch_id = ?)
                  AND (? IS NULL OR m.category_id = ?)
                  AND (? IS NULL OR m.status = ?)
                  AND m.is_deleted = 0
                  AND (
                        ? IS NULL
                        OR m.medicine_name LIKE ?
                        OR m.medicine_code LIKE ?
                        OR m.generic_name LIKE ?
                        OR m.brand_name LIKE ?
                  )
                ORDER BY m.created_at DESC, m.id DESC
                LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                this::mapMedicine,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                categoryId,
                categoryId,
                statusValue,
                statusValue,
                searchValue,
                searchValue,
                searchValue,
                searchValue,
                searchValue,
                limit,
                offset
        );
    }

    public Optional<MedicineRecord> findMedicineById(Long medicineId, Long tenantId, Long hospitalId, Long branchId) {
        String sql = """
                SELECT
                    m.id, m.tenant_id, m.hospital_id, m.branch_id,
                    m.medicine_code, m.medicine_name, m.category_id,
                    c.category_name,
                    m.generic_name, m.brand_name, m.manufacturer,
                    m.medicine_type, m.strength, m.unit, m.hsn_code,
                    m.tax_rate, m.purchase_price, m.sale_price, m.mrp,
                    m.reorder_level, m.prescription_required, m.is_billable,
                    m.status, m.created_at, m.updated_at
                FROM medicines m
                LEFT JOIN medicine_categories c
                    ON c.id = m.category_id
                    AND c.tenant_id = m.tenant_id
                    AND c.hospital_id = m.hospital_id
                    AND c.is_deleted = 0
                WHERE m.id = ?
                  AND m.tenant_id = ?
                  AND m.hospital_id = ?
                  AND (? IS NULL OR m.branch_id = ?)
                  AND m.is_deleted = 0
                LIMIT 1
                """;

        List<MedicineRecord> result = jdbcTemplate.query(
                sql,
                this::mapMedicine,
                medicineId,
                tenantId,
                hospitalId,
                branchId,
                branchId
        );

        return result.stream().findFirst();
    }

    public int archiveMedicine(Long medicineId, Long tenantId, Long hospitalId, Long branchId, Long updatedBy) {
        String sql = """
                UPDATE medicines
                SET status = 'ARCHIVED',
                    is_deleted = 1,
                    updated_by = ?,
                    updated_at = NOW()
                WHERE id = ?
                  AND tenant_id = ?
                  AND hospital_id = ?
                  AND (? IS NULL OR branch_id = ?)
                  AND is_deleted = 0
                """;

        return jdbcTemplate.update(sql, updatedBy, medicineId, tenantId, hospitalId, branchId, branchId);
    }

    public Long createStockBatch(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long createdBy,
            CreateStockBatchRequest request
    ) {
        String sql = """
                INSERT INTO pharmacy_stock_batches (
                    tenant_id, hospital_id, branch_id, medicine_id,
                    batch_number, expiry_date, opening_quantity, current_quantity,
                    purchase_price, sale_price, mrp, supplier_name,
                    purchase_invoice_number, purchase_date, status,
                    created_by, created_at, is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                request.medicineId(),
                request.batchNumber(),
                request.expiryDate(),
                request.quantity(),
                request.quantity(),
                defaultBigDecimal(request.purchasePrice()),
                defaultBigDecimal(request.salePrice()),
                defaultBigDecimal(request.mrp()),
                request.supplierName(),
                request.purchaseInvoiceNumber(),
                request.purchaseDate(),
                createdBy
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void createStockMovement(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long medicineId,
            Long batchId,
            String movementType,
            String referenceType,
            Long referenceId,
            BigDecimal quantity,
            BigDecimal beforeQuantity,
            BigDecimal afterQuantity,
            String remarks,
            Long createdBy
    ) {
        String sql = """
                INSERT INTO pharmacy_stock_movements (
                    tenant_id, hospital_id, branch_id, medicine_id, batch_id,
                    movement_type, reference_type, reference_id,
                    quantity, before_quantity, after_quantity,
                    remarks, created_by, created_at, is_deleted
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0)
                """;

        jdbcTemplate.update(
                sql,
                tenantId,
                hospitalId,
                branchId,
                medicineId,
                batchId,
                movementType,
                referenceType,
                referenceId,
                quantity,
                beforeQuantity,
                afterQuantity,
                remarks,
                createdBy
        );
    }

    public List<StockBatchRecord> findStock(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long medicineId
    ) {
        String sql = """
                SELECT
                    sb.id, sb.tenant_id, sb.hospital_id, sb.branch_id,
                    sb.medicine_id, m.medicine_code, m.medicine_name,
                    sb.batch_number, sb.expiry_date,
                    sb.opening_quantity, sb.current_quantity,
                    sb.purchase_price, sb.sale_price, sb.mrp,
                    sb.supplier_name, sb.purchase_invoice_number, sb.purchase_date,
                    sb.status, sb.created_at, sb.updated_at
                FROM pharmacy_stock_batches sb
                INNER JOIN medicines m
                    ON m.id = sb.medicine_id
                    AND m.tenant_id = sb.tenant_id
                    AND m.hospital_id = sb.hospital_id
                    AND m.is_deleted = 0
                WHERE sb.tenant_id = ?
                  AND sb.hospital_id = ?
                  AND (? IS NULL OR sb.branch_id = ?)
                  AND (? IS NULL OR sb.medicine_id = ?)
                  AND sb.is_deleted = 0
                ORDER BY sb.expiry_date ASC, sb.id ASC
                """;

        return jdbcTemplate.query(
                sql,
                this::mapStockBatch,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                medicineId,
                medicineId
        );
    }

    public List<StockBatchRecord> findLowStock(Long tenantId, Long hospitalId, Long branchId) {
        String sql = """
                SELECT
                    sb.id, sb.tenant_id, sb.hospital_id, sb.branch_id,
                    sb.medicine_id, m.medicine_code, m.medicine_name,
                    sb.batch_number, sb.expiry_date,
                    sb.opening_quantity, sb.current_quantity,
                    sb.purchase_price, sb.sale_price, sb.mrp,
                    sb.supplier_name, sb.purchase_invoice_number, sb.purchase_date,
                    sb.status, sb.created_at, sb.updated_at
                FROM pharmacy_stock_batches sb
                INNER JOIN medicines m
                    ON m.id = sb.medicine_id
                    AND m.tenant_id = sb.tenant_id
                    AND m.hospital_id = sb.hospital_id
                    AND m.is_deleted = 0
                WHERE sb.tenant_id = ?
                  AND sb.hospital_id = ?
                  AND (? IS NULL OR sb.branch_id = ?)
                  AND sb.current_quantity <= m.reorder_level
                  AND sb.is_deleted = 0
                ORDER BY sb.current_quantity ASC
                """;

        return jdbcTemplate.query(sql, this::mapStockBatch, tenantId, hospitalId, branchId, branchId);
    }

    public List<StockBatchRecord> findExpiringStock(Long tenantId, Long hospitalId, Long branchId, int days) {
        String sql = """
                SELECT
                    sb.id, sb.tenant_id, sb.hospital_id, sb.branch_id,
                    sb.medicine_id, m.medicine_code, m.medicine_name,
                    sb.batch_number, sb.expiry_date,
                    sb.opening_quantity, sb.current_quantity,
                    sb.purchase_price, sb.sale_price, sb.mrp,
                    sb.supplier_name, sb.purchase_invoice_number, sb.purchase_date,
                    sb.status, sb.created_at, sb.updated_at
                FROM pharmacy_stock_batches sb
                INNER JOIN medicines m
                    ON m.id = sb.medicine_id
                    AND m.tenant_id = sb.tenant_id
                    AND m.hospital_id = sb.hospital_id
                    AND m.is_deleted = 0
                WHERE sb.tenant_id = ?
                  AND sb.hospital_id = ?
                  AND (? IS NULL OR sb.branch_id = ?)
                  AND sb.expiry_date IS NOT NULL
                  AND sb.expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL ? DAY)
                  AND sb.is_deleted = 0
                ORDER BY sb.expiry_date ASC
                """;

        return jdbcTemplate.query(sql, this::mapStockBatch, tenantId, hospitalId, branchId, branchId, days);
    }

    public Optional<StockBatchRecord> findStockBatchById(Long batchId, Long tenantId, Long hospitalId, Long branchId) {
        String sql = """
                SELECT
                    sb.id, sb.tenant_id, sb.hospital_id, sb.branch_id,
                    sb.medicine_id, m.medicine_code, m.medicine_name,
                    sb.batch_number, sb.expiry_date,
                    sb.opening_quantity, sb.current_quantity,
                    sb.purchase_price, sb.sale_price, sb.mrp,
                    sb.supplier_name, sb.purchase_invoice_number, sb.purchase_date,
                    sb.status, sb.created_at, sb.updated_at
                FROM pharmacy_stock_batches sb
                INNER JOIN medicines m
                    ON m.id = sb.medicine_id
                    AND m.tenant_id = sb.tenant_id
                    AND m.hospital_id = sb.hospital_id
                    AND m.is_deleted = 0
                WHERE sb.id = ?
                  AND sb.tenant_id = ?
                  AND sb.hospital_id = ?
                  AND (? IS NULL OR sb.branch_id = ?)
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

    public int updateBatchQuantity(
            Long batchId,
            Long tenantId,
            Long hospitalId,
            Long branchId,
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

        return jdbcTemplate.update(sql, newQuantity, updatedBy, batchId, tenantId, hospitalId, branchId, branchId);
    }

    public List<StockMovementRecord> findStockMovements(
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long medicineId,
            Long batchId,
            int limit,
            int offset
    ) {
        String sql = """
                SELECT
                    sm.id, sm.tenant_id, sm.hospital_id, sm.branch_id,
                    sm.medicine_id, m.medicine_name,
                    sm.batch_id, sb.batch_number,
                    sm.movement_type, sm.reference_type, sm.reference_id,
                    sm.quantity, sm.before_quantity, sm.after_quantity,
                    sm.remarks, sm.created_at
                FROM pharmacy_stock_movements sm
                INNER JOIN medicines m
                    ON m.id = sm.medicine_id
                    AND m.tenant_id = sm.tenant_id
                    AND m.hospital_id = sm.hospital_id
                LEFT JOIN pharmacy_stock_batches sb
                    ON sb.id = sm.batch_id
                    AND sb.tenant_id = sm.tenant_id
                    AND sb.hospital_id = sm.hospital_id
                WHERE sm.tenant_id = ?
                  AND sm.hospital_id = ?
                  AND (? IS NULL OR sm.branch_id = ?)
                  AND (? IS NULL OR sm.medicine_id = ?)
                  AND (? IS NULL OR sm.batch_id = ?)
                  AND sm.is_deleted = 0
                ORDER BY sm.created_at DESC, sm.id DESC
                LIMIT ? OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                this::mapStockMovement,
                tenantId,
                hospitalId,
                branchId,
                branchId,
                medicineId,
                medicineId,
                batchId,
                batchId,
                limit,
                offset
        );
    }

    private CategoryRecord mapCategory(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private MedicineRecord mapMedicine(ResultSet rs, int rowNum) throws SQLException {
        return new MedicineRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                rs.getString("medicine_code"),
                rs.getString("medicine_name"),
                getNullableLong(rs, "category_id"),
                rs.getString("category_name"),
                rs.getString("generic_name"),
                rs.getString("brand_name"),
                rs.getString("manufacturer"),
                rs.getString("medicine_type"),
                rs.getString("strength"),
                rs.getString("unit"),
                rs.getString("hsn_code"),
                rs.getBigDecimal("tax_rate"),
                rs.getBigDecimal("purchase_price"),
                rs.getBigDecimal("sale_price"),
                rs.getBigDecimal("mrp"),
                rs.getBigDecimal("reorder_level"),
                rs.getInt("prescription_required") == 1,
                rs.getInt("is_billable") == 1,
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private StockBatchRecord mapStockBatch(ResultSet rs, int rowNum) throws SQLException {
        return new StockBatchRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                rs.getLong("medicine_id"),
                rs.getString("medicine_code"),
                rs.getString("medicine_name"),
                rs.getString("batch_number"),
                rs.getDate("expiry_date") == null ? null : rs.getDate("expiry_date").toLocalDate(),
                rs.getBigDecimal("opening_quantity"),
                rs.getBigDecimal("current_quantity"),
                rs.getBigDecimal("purchase_price"),
                rs.getBigDecimal("sale_price"),
                rs.getBigDecimal("mrp"),
                rs.getString("supplier_name"),
                rs.getString("purchase_invoice_number"),
                rs.getDate("purchase_date") == null ? null : rs.getDate("purchase_date").toLocalDate(),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime().toString()
        );
    }

    private StockMovementRecord mapStockMovement(ResultSet rs, int rowNum) throws SQLException {
        return new StockMovementRecord(
                rs.getLong("id"),
                rs.getLong("tenant_id"),
                rs.getLong("hospital_id"),
                getNullableLong(rs, "branch_id"),
                rs.getLong("medicine_id"),
                rs.getString("medicine_name"),
                getNullableLong(rs, "batch_id"),
                rs.getString("batch_number"),
                rs.getString("movement_type"),
                rs.getString("reference_type"),
                getNullableLong(rs, "reference_id"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("before_quantity"),
                rs.getBigDecimal("after_quantity"),
                rs.getString("remarks"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime().toString()
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record CategoryRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String categoryCode,
            String categoryName,
            String description,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record MedicineRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            String medicineCode,
            String medicineName,
            Long categoryId,
            String categoryName,
            String genericName,
            String brandName,
            String manufacturer,
            String medicineType,
            String strength,
            String unit,
            String hsnCode,
            BigDecimal taxRate,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            BigDecimal mrp,
            BigDecimal reorderLevel,
            boolean prescriptionRequired,
            boolean billable,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record StockBatchRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long medicineId,
            String medicineCode,
            String medicineName,
            String batchNumber,
            LocalDate expiryDate,
            BigDecimal openingQuantity,
            BigDecimal currentQuantity,
            BigDecimal purchasePrice,
            BigDecimal salePrice,
            BigDecimal mrp,
            String supplierName,
            String purchaseInvoiceNumber,
            LocalDate purchaseDate,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record StockMovementRecord(
            Long id,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long medicineId,
            String medicineName,
            Long batchId,
            String batchNumber,
            String movementType,
            String referenceType,
            Long referenceId,
            BigDecimal quantity,
            BigDecimal beforeQuantity,
            BigDecimal afterQuantity,
            String remarks,
            String createdAt
    ) {
    }
}