package com.plasmit.diagnostic.quality.quality.validator;

import com.plasmit.diagnostic.quality.common.exception.ApiException;
import com.plasmit.diagnostic.quality.quality.dto.request.*;
import com.plasmit.diagnostic.quality.validator.DateRangeValidator;
import com.plasmit.diagnostic.quality.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Component
public class DiagnosticQualityValidator {

    private static final Set<String> EQUIPMENT_STATUSES = Set.of(
            "Operational", "Maintenance", "CalibrationDue", "Down"
    );

    private static final Set<String> MAINTENANCE_STATUSES = Set.of(
            "Scheduled", "InProgress", "Completed"
    );

    private static final Set<String> QC_STATUSES = Set.of(
            "Pending", "Passed", "Failed"
    );

    private static final Set<String> STOCK_MOVEMENTS = Set.of(
            "IN", "OUT", "ADJUSTMENT"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public DiagnosticQualityValidator(DateRangeValidator dateRangeValidator,
                                      PaginationValidator paginationValidator) {
        this.dateRangeValidator = dateRangeValidator;
        this.paginationValidator = paginationValidator;
    }

    public void validateList(LocalDate fromDate, LocalDate toDate, Integer page, Integer limit) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
        paginationValidator.validate(page, limit);
    }

    public void validateEquipmentStatus(String status) {
        if (status != null && !status.isBlank() && !EQUIPMENT_STATUSES.contains(status)) {
            throw ApiException.validation("Invalid equipment status.");
        }
    }

    public void validateId(Long id, String name) {
        if (id == null || id <= 0) {
            throw ApiException.validation("Valid " + name + " is required.");
        }
    }

    public void validateMaintenance(CreateMaintenanceRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }
        validateId(request.equipmentId(), "equipmentId");
        if (request.maintenanceType() == null || request.maintenanceType().isBlank()) {
            throw ApiException.validation("maintenanceType is required.");
        }
    }

    public void validateCompleteMaintenance(CompleteMaintenanceRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }
        if (request.performedBy() == null || request.performedBy().isBlank()) {
            throw ApiException.validation("performedBy is required.");
        }
        if (request.resolutionNotes() == null || request.resolutionNotes().isBlank()) {
            throw ApiException.validation("resolutionNotes is required.");
        }
    }

    public void validateCalibration(CreateCalibrationRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }
        validateId(request.equipmentId(), "equipmentId");
        if (request.calibrationStatus() == null || request.calibrationStatus().isBlank()) {
            throw ApiException.validation("calibrationStatus is required.");
        }
        if (!QC_STATUSES.contains(request.calibrationStatus())) {
            throw ApiException.validation("calibrationStatus must be Passed, Failed or Pending.");
        }
        if (request.calibratedAt() == null) {
            throw ApiException.validation("calibratedAt is required.");
        }
    }

    public void validateQc(CreateQcEventRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }
        if (request.qcType() == null || request.qcType().isBlank()) {
            throw ApiException.validation("qcType is required.");
        }
        if (request.department() == null || request.department().isBlank()) {
            throw ApiException.validation("department is required.");
        }
        if (request.results() == null || request.results().isEmpty()) {
            throw ApiException.validation("At least one QC result is required.");
        }
        for (QcResultRequest result : request.results()) {
            if (result.parameterName() == null || result.parameterName().isBlank()) {
                throw ApiException.validation("parameterName is required.");
            }
            if (result.resultStatus() == null || result.resultStatus().isBlank()) {
                throw ApiException.validation("resultStatus is required.");
            }
            if (!QC_STATUSES.contains(result.resultStatus())) {
                throw ApiException.validation("Invalid QC resultStatus.");
            }
        }
    }

    public void validateStockMovement(StockMovementRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }
        validateId(request.itemId(), "itemId");
        if (request.movementType() == null || request.movementType().isBlank()) {
            throw ApiException.validation("movementType is required.");
        }
        if (!STOCK_MOVEMENTS.contains(request.movementType())) {
            throw ApiException.validation("movementType must be IN, OUT or ADJUSTMENT.");
        }
        validateQuantity(request.quantity(), "quantity");
    }

    public void validateConsumption(InventoryConsumptionRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }
        validateId(request.itemId(), "itemId");
        validateQuantity(request.quantityConsumed(), "quantityConsumed");
    }

    private void validateQuantity(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.validation(name + " must be greater than 0.");
        }
    }
}