package com.plasmit.pharmacy.hospital.validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.plasmit.pharmacy.hospital.exception.ApiException;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.CategoryRecord;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.MedicineRecord;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.StockBatchRecord;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.StockMovementRecord;
import com.plasmit.pharmacy.hospital.service.PharmacyService.AdjustStockRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.CreateCategoryRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.CreateMedicineRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.CreateStockBatchRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.UpdateMedicineRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PharmacyValidator {

    private static final Logger log = LoggerFactory.getLogger(PharmacyValidator.class);

    public void validateTenantHospitalContext(Long tenantId, Long hospitalId) {

        if (tenantId == null || hospitalId == null) {
            log.warn(
                    "Pharmacy validation failed. reason=TENANT_OR_HOSPITAL_MISSING tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (tenantId <= 0 || hospitalId <= 0) {
            log.warn(
                    "Pharmacy validation failed. reason=INVALID_TENANT_OR_HOSPITAL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateBranchContext(Long branchId) {

        if (branchId != null && branchId <= 0) {
            log.warn("Pharmacy validation failed. reason=INVALID_BRANCH_ID branchId={}", branchId);
            throw ApiException.badRequest("Invalid branch id.");
        }
    }

    public int validatePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public int validateSize(Integer size) {

        if (size == null || size < 1) {
            return 20;
        }

        if (size > 100) {
            log.warn("Pharmacy validation warning. reason=PAGE_SIZE_LIMIT_EXCEEDED requestedSize={}", size);
            return 100;
        }

        return size;
    }

    public int validateExpiringDays(Integer days) {

        if (days == null || days < 1) {
            return 30;
        }

        if (days > 365) {
            log.warn("Pharmacy validation warning. reason=EXPIRING_DAYS_LIMIT_EXCEEDED requestedDays={}", days);
            return 365;
        }

        return days;
    }

    public void validateStatusFilter(String status) {

        if (status == null || status.isBlank()) {
            return;
        }

        if (!status.equalsIgnoreCase("ACTIVE")
                && !status.equalsIgnoreCase("INACTIVE")
                && !status.equalsIgnoreCase("ARCHIVED")) {
            log.warn("Pharmacy validation failed. reason=INVALID_STATUS status={}", status);
            throw ApiException.badRequest("Status must be ACTIVE, INACTIVE or ARCHIVED.");
        }
    }

    public void validateCategoryId(Long categoryId) {

        if (categoryId != null && categoryId <= 0) {
            log.warn("Pharmacy validation failed. reason=INVALID_CATEGORY_ID categoryId={}", categoryId);
            throw ApiException.badRequest("Invalid category id.");
        }
    }

    public void validateMedicineId(Long medicineId) {

        if (medicineId == null || medicineId <= 0) {
            log.warn("Pharmacy validation failed. reason=INVALID_MEDICINE_ID medicineId={}", medicineId);
            throw ApiException.badRequest("Invalid medicine id.");
        }
    }

    public void validateBatchId(Long batchId) {

        if (batchId == null || batchId <= 0) {
            log.warn("Pharmacy validation failed. reason=INVALID_BATCH_ID batchId={}", batchId);
            throw ApiException.badRequest("Invalid stock batch id.");
        }
    }

    public void validateCategoryExists(
            boolean exists,
            Long categoryId,
            Long tenantId,
            Long hospitalId
    ) {

        if (!exists) {
            log.warn(
                    "Pharmacy validation failed. reason=CATEGORY_NOT_FOUND categoryId={} tenantId={} hospitalId={}",
                    categoryId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Medicine category not found.");
        }
    }

    public void validateMedicineFound(
            MedicineRecord medicine,
            Long medicineId,
            Long tenantId,
            Long hospitalId
    ) {

        if (medicine == null) {
            log.warn(
                    "Pharmacy validation failed. reason=MEDICINE_NOT_FOUND medicineId={} tenantId={} hospitalId={}",
                    medicineId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Medicine not found.");
        }
    }

    public void validateStockBatchFound(
            StockBatchRecord batch,
            Long batchId,
            Long tenantId,
            Long hospitalId
    ) {

        if (batch == null) {
            log.warn(
                    "Pharmacy validation failed. reason=STOCK_BATCH_NOT_FOUND batchId={} tenantId={} hospitalId={}",
                    batchId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Stock batch not found.");
        }
    }

    public void validateCategoryList(List<CategoryRecord> categories, Long tenantId, Long hospitalId) {

        if (categories == null) {
            log.warn(
                    "Pharmacy validation failed. reason=CATEGORY_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch medicine categories.");
        }
    }

    public void validateMedicineList(List<MedicineRecord> medicines, Long tenantId, Long hospitalId) {

        if (medicines == null) {
            log.warn(
                    "Pharmacy validation failed. reason=MEDICINE_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch medicines.");
        }
    }

    public void validateStockBatchList(List<StockBatchRecord> batches, Long tenantId, Long hospitalId) {

        if (batches == null) {
            log.warn(
                    "Pharmacy validation failed. reason=STOCK_BATCH_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch pharmacy stock.");
        }
    }

    public void validateStockMovementList(List<StockMovementRecord> movements, Long tenantId, Long hospitalId) {

        if (movements == null) {
            log.warn(
                    "Pharmacy validation failed. reason=STOCK_MOVEMENT_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch stock movements.");
        }
    }

    public void validateCreateCategoryRequest(CreateCategoryRequest request) {

        if (request == null) {
            log.warn("Pharmacy validation failed. reason=CREATE_CATEGORY_REQUEST_NULL");
            throw ApiException.badRequest("Category request is required.");
        }

        if (request.categoryName() == null || request.categoryName().isBlank()) {
            log.warn("Pharmacy validation failed. reason=CATEGORY_NAME_REQUIRED");
            throw ApiException.badRequest("Category name is required.");
        }
    }

    public void validateCreateMedicineRequest(CreateMedicineRequest request) {

        if (request == null) {
            log.warn("Pharmacy validation failed. reason=CREATE_MEDICINE_REQUEST_NULL");
            throw ApiException.badRequest("Medicine request is required.");
        }

        if (request.medicineName() == null || request.medicineName().isBlank()) {
            log.warn("Pharmacy validation failed. reason=MEDICINE_NAME_REQUIRED");
            throw ApiException.badRequest("Medicine name is required.");
        }

        validateCategoryId(request.categoryId());
        validateAmount(request.taxRate(), "Tax rate");
        validateAmount(request.purchasePrice(), "Purchase price");
        validateAmount(request.salePrice(), "Sale price");
        validateAmount(request.mrp(), "MRP");
        validateAmount(request.reorderLevel(), "Reorder level");
    }

    public void validateUpdateMedicineRequest(UpdateMedicineRequest request) {

        if (request == null) {
            log.warn("Pharmacy validation failed. reason=UPDATE_MEDICINE_REQUEST_NULL");
            throw ApiException.badRequest("Medicine request is required.");
        }

        if (request.medicineName() == null || request.medicineName().isBlank()) {
            log.warn("Pharmacy validation failed. reason=MEDICINE_NAME_REQUIRED");
            throw ApiException.badRequest("Medicine name is required.");
        }

        validateCategoryId(request.categoryId());
        validateAmount(request.taxRate(), "Tax rate");
        validateAmount(request.purchasePrice(), "Purchase price");
        validateAmount(request.salePrice(), "Sale price");
        validateAmount(request.mrp(), "MRP");
        validateAmount(request.reorderLevel(), "Reorder level");
    }

    public void validateCreateStockBatchRequest(CreateStockBatchRequest request) {

        if (request == null) {
            log.warn("Pharmacy validation failed. reason=CREATE_STOCK_BATCH_REQUEST_NULL");
            throw ApiException.badRequest("Stock batch request is required.");
        }

        validateMedicineId(request.medicineId());

        if (request.batchNumber() == null || request.batchNumber().isBlank()) {
            log.warn("Pharmacy validation failed. reason=BATCH_NUMBER_REQUIRED");
            throw ApiException.badRequest("Batch number is required.");
        }

        validatePositiveQuantity(request.quantity(), "Stock quantity");
        validateExpiryDate(request.expiryDate());

        validateAmount(request.purchasePrice(), "Purchase price");
        validateAmount(request.salePrice(), "Sale price");
        validateAmount(request.mrp(), "MRP");
    }

    public void validateAdjustStockRequest(AdjustStockRequest request) {

        if (request == null) {
            log.warn("Pharmacy validation failed. reason=ADJUST_STOCK_REQUEST_NULL");
            throw ApiException.badRequest("Stock adjustment request is required.");
        }

        validateBatchId(request.batchId());

        if (request.movementType() == null || request.movementType().isBlank()) {
            log.warn("Pharmacy validation failed. reason=MOVEMENT_TYPE_REQUIRED");
            throw ApiException.badRequest("Movement type is required.");
        }

        validateMovementType(request.movementType());
        validatePositiveQuantity(request.quantity(), "Quantity");
    }

    public void validateMovementType(String movementType) {

        if (movementType == null || movementType.isBlank()) {
            log.warn("Pharmacy validation failed. reason=MOVEMENT_TYPE_REQUIRED");
            throw ApiException.badRequest("Movement type is required.");
        }

        String value = movementType.trim().toUpperCase();

        if (!value.equals("STOCK_IN")
                && !value.equals("STOCK_OUT")
                && !value.equals("ADJUSTMENT")) {
            log.warn("Pharmacy validation failed. reason=INVALID_MOVEMENT_TYPE movementType={}", movementType);
            throw ApiException.badRequest("Invalid movement type. Use STOCK_IN, STOCK_OUT, or ADJUSTMENT.");
        }
    }

    public String normalizeMovementType(String movementType) {
        validateMovementType(movementType);
        return movementType.trim().toUpperCase();
    }

    public void validateSufficientStock(
            BigDecimal beforeQuantity,
            BigDecimal requestedQuantity,
            Long batchId
    ) {

        if (beforeQuantity == null) {
            log.warn("Pharmacy validation failed. reason=BATCH_CURRENT_QUANTITY_NULL batchId={}", batchId);
            throw ApiException.badRequest("Invalid stock quantity.");
        }

        if (beforeQuantity.compareTo(requestedQuantity) < 0) {
            log.warn(
                    "Pharmacy validation failed. reason=INSUFFICIENT_STOCK batchId={} currentQuantity={} requestedQuantity={}",
                    batchId,
                    beforeQuantity,
                    requestedQuantity
            );
            throw ApiException.badRequest("Insufficient stock.");
        }
    }

    public void validateUpdateCount(
            int updated,
            Long id,
            String entity,
            Long tenantId,
            Long hospitalId
    ) {

        if (updated == 0) {
            log.warn(
                    "Pharmacy validation failed. reason={}_UPDATE_FAILED id={} tenantId={} hospitalId={}",
                    entity,
                    id,
                    tenantId,
                    hospitalId
            );

            if ("MEDICINE".equals(entity)) {
                throw ApiException.notFound("Medicine not found.");
            }

            throw ApiException.notFound("Stock batch not found.");
        }
    }

    public void validateArchiveCount(
            int updated,
            Long id,
            String entity,
            Long tenantId,
            Long hospitalId
    ) {

        if (updated == 0) {
            log.warn(
                    "Pharmacy validation failed. reason={}_ARCHIVE_FAILED id={} tenantId={} hospitalId={}",
                    entity,
                    id,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Medicine not found.");
        }
    }

    private void validateAmount(BigDecimal amount, String fieldName) {

        if (amount == null) {
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Pharmacy validation failed. reason=NEGATIVE_AMOUNT field={} amount={}", fieldName, amount);
            throw ApiException.badRequest(fieldName + " cannot be negative.");
        }
    }

    private void validatePositiveQuantity(BigDecimal quantity, String fieldName) {

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Pharmacy validation failed. reason=INVALID_QUANTITY field={} quantity={}", fieldName, quantity);
            throw ApiException.badRequest(fieldName + " must be greater than zero.");
        }
    }

    private void validateExpiryDate(LocalDate expiryDate) {

        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            log.warn("Pharmacy validation failed. reason=PAST_EXPIRY_DATE expiryDate={}", expiryDate);
            throw ApiException.badRequest("Expiry date cannot be in the past.");
        }
    }
}