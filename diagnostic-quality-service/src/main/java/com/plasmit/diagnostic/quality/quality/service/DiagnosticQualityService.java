package com.plasmit.diagnostic.quality.quality.service;

import com.plasmit.diagnostic.quality.context.TenantContext;
import com.plasmit.diagnostic.quality.quality.dto.request.*;
import com.plasmit.diagnostic.quality.quality.dto.response.*;
import com.plasmit.diagnostic.quality.quality.repository.DiagnosticQualityRepository;
import com.plasmit.diagnostic.quality.quality.validator.DiagnosticQualityValidator;
import com.plasmit.diagnostic.quality.validator.CommonRequestValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DiagnosticQualityService {

    private final DiagnosticQualityRepository repository;
    private final DiagnosticQualityValidator validator;
    private final CommonRequestValidator commonRequestValidator;

    public DiagnosticQualityService(DiagnosticQualityRepository repository,
                                    DiagnosticQualityValidator validator,
                                    CommonRequestValidator commonRequestValidator) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
    }

    public List<EquipmentResponse> getEquipments(String status, String department) {
        validateContext();
        validator.validateEquipmentStatus(status);

        return repository.findEquipments(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                status,
                department
        );
    }

    public EquipmentResponse getEquipment(Long equipmentId) {
        validateContext();
        validator.validateId(equipmentId, "equipmentId");

        return repository.findEquipmentById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                equipmentId
        );
    }

    @Transactional
    public MaintenanceResponse createMaintenance(CreateMaintenanceRequest request) {
        validateContext();
        validator.validateMaintenance(request);

        MaintenanceResponse response = repository.createMaintenance(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                request
        );

        audit("EQUIPMENT_MAINTENANCE", response.maintenanceId(), "MAINTENANCE_CREATED",
                null, response.maintenanceStatus(), "Maintenance created.");

        return response;
    }

    @Transactional
    public MaintenanceResponse completeMaintenance(Long maintenanceId, CompleteMaintenanceRequest request) {
        validateContext();
        validator.validateId(maintenanceId, "maintenanceId");
        validator.validateCompleteMaintenance(request);

        MaintenanceResponse before = repository.findMaintenanceById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                maintenanceId
        );

        MaintenanceResponse response = repository.completeMaintenance(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                maintenanceId,
                TenantContext.getUserId(),
                request
        );

        audit("EQUIPMENT_MAINTENANCE", response.maintenanceId(), "MAINTENANCE_COMPLETED",
                before.maintenanceStatus(), response.maintenanceStatus(), request.resolutionNotes());

        return response;
    }

    @Transactional
    public CalibrationResponse createCalibration(CreateCalibrationRequest request) {
        validateContext();
        validator.validateCalibration(request);

        CalibrationResponse response = repository.createCalibration(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                request
        );

        audit("EQUIPMENT_CALIBRATION", response.calibrationId(), "CALIBRATION_CREATED",
                null, response.calibrationStatus(), request.remarks());

        return response;
    }

    @Transactional
    public QcEventResponse createQcEvent(CreateQcEventRequest request) {
        validateContext();
        validator.validateQc(request);

        QcEventResponse response = repository.createQcEvent(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                request,
                "QC-" + System.currentTimeMillis()
        );

        audit("QUALITY_CONTROL", response.qcEventId(), "QC_EVENT_CREATED",
                null, response.qcStatus(), request.remarks());

        return response;
    }

    public List<QcEventResponse> getQcEvents(LocalDate fromDate, LocalDate toDate, String status,
                                             Integer page, Integer limit) {
        validateContext();
        validator.validateList(fromDate, toDate, page, limit);

        return repository.findQcEvents(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                fromDate,
                toDate,
                status
        );
    }

    public List<InventoryItemResponse> getInventoryItems(Boolean lowStockOnly) {
        validateContext();

        return repository.findInventoryItems(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                lowStockOnly
        );
    }

    @Transactional
    public StockMovementResponse stockMovement(StockMovementRequest request) {
        validateContext();
        validator.validateStockMovement(request);

        StockMovementResponse response = repository.stockMovement(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                request
        );

        audit("INVENTORY_ITEM", request.itemId(), "STOCK_" + request.movementType(),
                null, null, request.remarks());

        return response;
    }

    @Transactional
    public InventoryConsumptionResponse consumeInventory(InventoryConsumptionRequest request) {
        validateContext();
        validator.validateConsumption(request);

        InventoryConsumptionResponse response = repository.consumeInventory(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                TenantContext.getUserId(),
                request
        );

        audit("INVENTORY_CONSUMPTION", response.consumptionId(), "INVENTORY_CONSUMED",
                null, null, request.remarks());

        return response;
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }

    private void audit(String entityType, Long entityId, String eventType,
                       String fromStatus, String toStatus, String notes) {
        repository.insertAudit(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                entityType,
                entityId,
                eventType,
                fromStatus,
                toStatus,
                notes,
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );
    }
}