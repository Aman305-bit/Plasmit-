package com.plasmit.pharmacy.hospital.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.plasmit.pharmacy.hospital.exception.ApiException;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.CategoryRecord;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.MedicineRecord;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.StockBatchRecord;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.StockMovementRecord;
import com.plasmit.pharmacy.hospital.security.SecurityConfig.CurrentUser;
import com.plasmit.pharmacy.hospital.security.SecurityConfig.TenantContext;
import com.plasmit.pharmacy.hospital.validator.PharmacyValidator;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PharmacyService {

    private static final Logger log = LoggerFactory.getLogger(PharmacyService.class);

    private final PharmacyRepository repository;
    private final PharmacyValidator validator;

    public PharmacyService(
            PharmacyRepository repository,
            PharmacyValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<CategoryRecord> listCategories(String status) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateStatusFilter(status);

        requirePermission(currentUser, "pharmacy.view");

        log.info("List medicine categories request. tenantId={} hospitalId={} branchId={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId());

        List<CategoryRecord> categories = repository.findCategories(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                status
        );

        validator.validateCategoryList(
                categories,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return categories;
    }

    @Transactional
    public CategoryRecord createCategory(CreateCategoryRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateCategoryRequest(request);

        requirePermission(currentUser, "pharmacy.create");

        String categoryCode = repository.generateNextCategoryCode(currentUser.getHospitalId());

        Long categoryId = repository.createCategory(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                categoryCode,
                request
        );

        log.info("Medicine category created. categoryId={} categoryCode={}",
                categoryId,
                categoryCode);

        List<CategoryRecord> categories = repository.findCategories(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                "ACTIVE"
        );

        validator.validateCategoryList(
                categories,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return categories
                .stream()
                .filter(category -> category.id().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Category not found."));
    }

    public List<MedicineRecord> listMedicines(
            String query,
            Long categoryId,
            String status,
            Integer page,
            Integer size
    ) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCategoryId(categoryId);
        validator.validateStatusFilter(status);

        requirePermission(currentUser, "pharmacy.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        log.info("List medicines request. tenantId={} hospitalId={} branchId={} query={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query);

        List<MedicineRecord> medicines = repository.findMedicines(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query,
                categoryId,
                status,
                safeSize,
                offset
        );

        validator.validateMedicineList(
                medicines,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return medicines;
    }

    public MedicineRecord getMedicine(Long medicineId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateMedicineId(medicineId);

        requirePermission(currentUser, "pharmacy.view");

        MedicineRecord medicine = repository.findMedicineById(
                medicineId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateMedicineFound(
                medicine,
                medicineId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return medicine;
    }

    @Transactional
    public MedicineRecord createMedicine(CreateMedicineRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateMedicineRequest(request);

        requirePermission(currentUser, "pharmacy.create");

        validateCategory(
                currentUser,
                request.categoryId()
        );

        String medicineCode = repository.generateNextMedicineCode(currentUser.getHospitalId());

        log.info("Create medicine request. tenantId={} hospitalId={} medicineName={} medicineCode={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.medicineName(),
                medicineCode);

        Long medicineId = repository.createMedicine(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                medicineCode,
                request
        );

        log.info("Medicine created successfully. medicineId={} medicineCode={}",
                medicineId,
                medicineCode);

        return getMedicine(medicineId);
    }

    @Transactional
    public MedicineRecord updateMedicine(
            Long medicineId,
            UpdateMedicineRequest request
    ) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateMedicineId(medicineId);
        validator.validateUpdateMedicineRequest(request);

        requirePermission(currentUser, "pharmacy.update");

        MedicineRecord existing = repository.findMedicineById(
                medicineId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateMedicineFound(
                existing,
                medicineId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validateCategory(
                currentUser,
                request.categoryId()
        );

        int updated = repository.updateMedicine(
                medicineId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request
        );

        validator.validateUpdateCount(
                updated,
                medicineId,
                "MEDICINE",
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Medicine updated successfully. medicineId={}", medicineId);

        return getMedicine(medicineId);
    }

    @Transactional
    public Map<String, Object> archiveMedicine(Long medicineId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateMedicineId(medicineId);

        requirePermission(currentUser, "pharmacy.archive");

        int updated = repository.archiveMedicine(
                medicineId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId()
        );

        validator.validateArchiveCount(
                updated,
                medicineId,
                "MEDICINE",
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Medicine archived successfully. medicineId={}", medicineId);

        return Map.of(
                "medicineId", medicineId,
                "archived", true
        );
    }

    @Transactional
    public StockBatchRecord createStockBatch(CreateStockBatchRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateStockBatchRequest(request);

        requirePermission(currentUser, "pharmacy.stock.manage");

        MedicineRecord medicine = repository.findMedicineById(
                request.medicineId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateMedicineFound(
                medicine,
                request.medicineId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        Long batchId = repository.createStockBatch(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request
        );

        repository.createStockMovement(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.medicineId(),
                batchId,
                "PURCHASE",
                "MANUAL",
                null,
                request.quantity(),
                BigDecimal.ZERO,
                request.quantity(),
                "New stock batch added.",
                currentUser.getUserId()
        );

        log.info("Stock batch created successfully. batchId={} medicineId={} quantity={}",
                batchId,
                request.medicineId(),
                request.quantity());

        StockBatchRecord batch = repository.findStockBatchById(
                batchId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateStockBatchFound(
                batch,
                batchId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return batch;
    }

    public List<StockBatchRecord> listStock(Long medicineId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());

        if (medicineId != null) {
            validator.validateMedicineId(medicineId);
        }

        requirePermission(currentUser, "pharmacy.view");

        List<StockBatchRecord> stock = repository.findStock(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                medicineId
        );

        validator.validateStockBatchList(
                stock,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return stock;
    }

    public List<StockBatchRecord> lowStock() {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());

        requirePermission(currentUser, "pharmacy.view");

        List<StockBatchRecord> stock = repository.findLowStock(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        );

        validator.validateStockBatchList(
                stock,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return stock;
    }

    public List<StockBatchRecord> expiringStock(Integer days) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());

        requirePermission(currentUser, "pharmacy.view");

        int safeDays = validator.validateExpiringDays(days);

        List<StockBatchRecord> stock = repository.findExpiringStock(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                safeDays
        );

        validator.validateStockBatchList(
                stock,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return stock;
    }

    @Transactional
    public StockBatchRecord adjustStock(AdjustStockRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateAdjustStockRequest(request);

        requirePermission(currentUser, "pharmacy.stock.manage");

        StockBatchRecord batch = repository.findStockBatchById(
                request.batchId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateStockBatchFound(
                batch,
                request.batchId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        BigDecimal quantity = request.quantity();
        String movementType = validator.normalizeMovementType(request.movementType());

        BigDecimal beforeQuantity = batch.currentQuantity();
        BigDecimal afterQuantity;

        if ("STOCK_IN".equals(movementType)) {
            afterQuantity = beforeQuantity.add(quantity);
        } else if ("STOCK_OUT".equals(movementType)) {

            validator.validateSufficientStock(
                    beforeQuantity,
                    quantity,
                    batch.id()
            );

            afterQuantity = beforeQuantity.subtract(quantity);
        } else {
            afterQuantity = quantity;
        }

        int updated = repository.updateBatchQuantity(
                batch.id(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                afterQuantity,
                currentUser.getUserId()
        );

        validator.validateUpdateCount(
                updated,
                batch.id(),
                "STOCK_BATCH",
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        repository.createStockMovement(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                batch.medicineId(),
                batch.id(),
                movementType,
                request.referenceType(),
                request.referenceId(),
                quantity,
                beforeQuantity,
                afterQuantity,
                request.remarks(),
                currentUser.getUserId()
        );

        log.info("Stock adjusted successfully. batchId={} movementType={} before={} after={}",
                batch.id(),
                movementType,
                beforeQuantity,
                afterQuantity);

        StockBatchRecord updatedBatch = repository.findStockBatchById(
                batch.id(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateStockBatchFound(
                updatedBatch,
                batch.id(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return updatedBatch;
    }

    public List<StockMovementRecord> stockMovements(
            Long medicineId,
            Long batchId,
            Integer page,
            Integer size
    ) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());

        if (medicineId != null) {
            validator.validateMedicineId(medicineId);
        }

        if (batchId != null) {
            validator.validateBatchId(batchId);
        }

        requirePermission(currentUser, "pharmacy.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        List<StockMovementRecord> movements = repository.findStockMovements(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                medicineId,
                batchId,
                safeSize,
                offset
        );

        validator.validateStockMovementList(
                movements,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return movements;
    }

    private void validateCategory(
            CurrentUser currentUser,
            Long categoryId
    ) {

        validator.validateCategoryId(categoryId);

        boolean exists = repository.categoryExists(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                categoryId
        );

        validator.validateCategoryExists(
                exists,
                categoryId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );
    }

    private CurrentUser requireUser() {

        CurrentUser currentUser = TenantContext.get();

        if (currentUser == null) {
            throw ApiException.unauthorized("Authentication required.");
        }

        return currentUser;
    }

    private void requirePermission(
            CurrentUser currentUser,
            String permission
    ) {

        if (!currentUser.hasPermission(permission)) {
            log.warn("Permission denied. userId={} tenantId={} hospitalId={} permission={}",
                    currentUser.getUserId(),
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    permission);

            throw ApiException.forbidden("Permission denied.");
        }
    }

    public record CreateCategoryRequest(
            @NotBlank(message = "Category name is required.")
            String categoryName,

            String description
    ) {
    }

    public record CreateMedicineRequest(
            @NotBlank(message = "Medicine name is required.")
            String medicineName,

            Long categoryId,
            String genericName,
            String brandName,
            String manufacturer,
            String medicineType,
            String strength,
            String unit,
            String hsnCode,

            @DecimalMin(value = "0.0", message = "Tax rate cannot be negative.")
            BigDecimal taxRate,

            @DecimalMin(value = "0.0", message = "Purchase price cannot be negative.")
            BigDecimal purchasePrice,

            @DecimalMin(value = "0.0", message = "Sale price cannot be negative.")
            BigDecimal salePrice,

            @DecimalMin(value = "0.0", message = "MRP cannot be negative.")
            BigDecimal mrp,

            @DecimalMin(value = "0.0", message = "Reorder level cannot be negative.")
            BigDecimal reorderLevel,

            Boolean prescriptionRequired
    ) {
    }

    public record UpdateMedicineRequest(
            @NotBlank(message = "Medicine name is required.")
            String medicineName,

            Long categoryId,
            String genericName,
            String brandName,
            String manufacturer,
            String medicineType,
            String strength,
            String unit,
            String hsnCode,

            @DecimalMin(value = "0.0", message = "Tax rate cannot be negative.")
            BigDecimal taxRate,

            @DecimalMin(value = "0.0", message = "Purchase price cannot be negative.")
            BigDecimal purchasePrice,

            @DecimalMin(value = "0.0", message = "Sale price cannot be negative.")
            BigDecimal salePrice,

            @DecimalMin(value = "0.0", message = "MRP cannot be negative.")
            BigDecimal mrp,

            @DecimalMin(value = "0.0", message = "Reorder level cannot be negative.")
            BigDecimal reorderLevel,

            Boolean prescriptionRequired
    ) {
    }

    public record CreateStockBatchRequest(
            @NotNull(message = "Medicine id is required.")
            Long medicineId,

            @NotBlank(message = "Batch number is required.")
            String batchNumber,

            LocalDate expiryDate,

            @NotNull(message = "Quantity is required.")
            @DecimalMin(value = "0.01", message = "Quantity must be greater than zero.")
            BigDecimal quantity,

            @DecimalMin(value = "0.0", message = "Purchase price cannot be negative.")
            BigDecimal purchasePrice,

            @DecimalMin(value = "0.0", message = "Sale price cannot be negative.")
            BigDecimal salePrice,

            @DecimalMin(value = "0.0", message = "MRP cannot be negative.")
            BigDecimal mrp,

            String supplierName,
            String purchaseInvoiceNumber,
            LocalDate purchaseDate
    ) {
    }

    public record AdjustStockRequest(
            @NotNull(message = "Batch id is required.")
            Long batchId,

            @NotBlank(message = "Movement type is required.")
            String movementType,

            @NotNull(message = "Quantity is required.")
            @DecimalMin(value = "0.01", message = "Quantity must be greater than zero.")
            BigDecimal quantity,

            String referenceType,
            Long referenceId,
            String remarks
    ) {
    }
}