package com.plasmit.diagnostic.quality.quality.controller;

import com.plasmit.diagnostic.quality.common.response.ApiResponse;
import com.plasmit.diagnostic.quality.quality.dto.request.*;
import com.plasmit.diagnostic.quality.quality.dto.response.*;
import com.plasmit.diagnostic.quality.quality.service.DiagnosticQualityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital/diagnostic-quality")
@CrossOrigin("*")
public class DiagnosticQualityController {

    private final DiagnosticQualityService service;

    public DiagnosticQualityController(DiagnosticQualityService service) {
        this.service = service;
    }

    @GetMapping("/equipments")
    public ApiResponse<List<EquipmentResponse>> getEquipments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "department", required = false) String department
    ) {
        return ApiResponse.success(
                "Diagnostic equipments fetched successfully.",
                service.getEquipments(status, department)
        );
    }

    @GetMapping("/equipments/{equipmentId}")
    public ApiResponse<EquipmentResponse> getEquipment(@PathVariable Long equipmentId) {
        return ApiResponse.success(
                "Diagnostic equipment fetched successfully.",
                service.getEquipment(equipmentId)
        );
    }

    @PostMapping("/maintenance")
    public ApiResponse<MaintenanceResponse> createMaintenance(
            @Valid @RequestBody CreateMaintenanceRequest request
    ) {
        return ApiResponse.success(
                "Equipment maintenance created successfully.",
                service.createMaintenance(request)
        );
    }

    @PostMapping("/maintenance/{maintenanceId}/complete")
    public ApiResponse<MaintenanceResponse> completeMaintenance(
            @PathVariable Long maintenanceId,
            @Valid @RequestBody CompleteMaintenanceRequest request
    ) {
        return ApiResponse.success(
                "Equipment maintenance completed successfully.",
                service.completeMaintenance(maintenanceId, request)
        );
    }

    @PostMapping("/calibrations")
    public ApiResponse<CalibrationResponse> createCalibration(
            @Valid @RequestBody CreateCalibrationRequest request
    ) {
        return ApiResponse.success(
                "Equipment calibration created successfully.",
                service.createCalibration(request)
        );
    }

    @PostMapping("/qc-events")
    public ApiResponse<QcEventResponse> createQcEvent(
            @Valid @RequestBody CreateQcEventRequest request
    ) {
        return ApiResponse.success(
                "Quality control event created successfully.",
                service.createQcEvent(request)
        );
    }

    @GetMapping("/qc-events")
    public ApiResponse<List<QcEventResponse>> getQcEvents(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "status", required = false) String status,

            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "limit", defaultValue = "25") Integer limit
    ) {
        return ApiResponse.success(
                "Quality control events fetched successfully.",
                service.getQcEvents(fromDate, toDate, status, page, limit)
        );
    }

    @GetMapping("/inventory/items")
    public ApiResponse<List<InventoryItemResponse>> getInventoryItems(
            @RequestParam(value = "lowStockOnly", defaultValue = "false") Boolean lowStockOnly
    ) {
        return ApiResponse.success(
                "Diagnostic inventory items fetched successfully.",
                service.getInventoryItems(lowStockOnly)
        );
    }

    @PostMapping("/inventory/stock-movements")
    public ApiResponse<StockMovementResponse> stockMovement(
            @Valid @RequestBody StockMovementRequest request
    ) {
        return ApiResponse.success(
                "Inventory stock movement recorded successfully.",
                service.stockMovement(request)
        );
    }

    @PostMapping("/inventory/consumptions")
    public ApiResponse<InventoryConsumptionResponse> consumeInventory(
            @Valid @RequestBody InventoryConsumptionRequest request
    ) {
        return ApiResponse.success(
                "Inventory consumption recorded successfully.",
                service.consumeInventory(request)
        );
    }
}