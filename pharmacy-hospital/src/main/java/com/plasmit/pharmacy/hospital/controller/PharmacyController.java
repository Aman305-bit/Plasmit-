package com.plasmit.pharmacy.hospital.controller;

import java.util.List;
import java.util.Map;

import com.plasmit.pharmacy.hospital.common.ApiResponse;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.CategoryRecord;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.MedicineRecord;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.StockBatchRecord;
import com.plasmit.pharmacy.hospital.repository.PharmacyRepository.StockMovementRecord;
import com.plasmit.pharmacy.hospital.service.PharmacyService;
import com.plasmit.pharmacy.hospital.service.PharmacyService.AdjustStockRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.CreateCategoryRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.CreateMedicineRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.CreateStockBatchRequest;
import com.plasmit.pharmacy.hospital.service.PharmacyService.UpdateMedicineRequest;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hospital/pharmacy")
public class PharmacyController {

    private static final Logger log = LoggerFactory.getLogger(PharmacyController.class);

    private final PharmacyService service;

    public PharmacyController(PharmacyService service) {
        this.service = service;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryRecord>>> categories(
            @RequestParam(required = false) String status
    ) {
        log.info("Medicine categories API called.");

        List<CategoryRecord> response = service.listCategories(status);

        log.info("Medicine categories API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Medicine categories fetched successfully.", response));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryRecord>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        log.info("Create medicine category API called.");

        CategoryRecord response = service.createCategory(request);

        log.info("Create medicine category API completed. categoryId={}", response.id());

        return ResponseEntity.ok(ApiResponse.success("Medicine category created successfully.", response));
    }

    @GetMapping("/medicines")
    public ResponseEntity<ApiResponse<List<MedicineRecord>>> medicines(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        log.info("Medicines API called.");

        List<MedicineRecord> response = service.listMedicines(query, categoryId, status, page, size);

        log.info("Medicines API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Medicines fetched successfully.", response));
    }

    @PostMapping("/medicines")
    public ResponseEntity<ApiResponse<MedicineRecord>> createMedicine(
            @Valid @RequestBody CreateMedicineRequest request
    ) {
        log.info("Create medicine API called.");

        MedicineRecord response = service.createMedicine(request);

        log.info("Create medicine API completed. medicineId={}", response.id());

        return ResponseEntity.ok(ApiResponse.success("Medicine created successfully.", response));
    }

    @GetMapping("/medicines/{medicineId}")
    public ResponseEntity<ApiResponse<MedicineRecord>> medicineDetail(
            @PathVariable Long medicineId
    ) {
        log.info("Medicine detail API called. medicineId={}", medicineId);

        MedicineRecord response = service.getMedicine(medicineId);

        log.info("Medicine detail API completed. medicineId={}", medicineId);

        return ResponseEntity.ok(ApiResponse.success("Medicine fetched successfully.", response));
    }

    @PatchMapping("/medicines/{medicineId}")
    public ResponseEntity<ApiResponse<MedicineRecord>> updateMedicine(
            @PathVariable Long medicineId,
            @Valid @RequestBody UpdateMedicineRequest request
    ) {
        log.info("Update medicine API called. medicineId={}", medicineId);

        MedicineRecord response = service.updateMedicine(medicineId, request);

        log.info("Update medicine API completed. medicineId={}", medicineId);

        return ResponseEntity.ok(ApiResponse.success("Medicine updated successfully.", response));
    }

    @DeleteMapping("/medicines/{medicineId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> archiveMedicine(
            @PathVariable Long medicineId
    ) {
        log.info("Archive medicine API called. medicineId={}", medicineId);

        Map<String, Object> response = service.archiveMedicine(medicineId);

        log.info("Archive medicine API completed. medicineId={}", medicineId);

        return ResponseEntity.ok(ApiResponse.success("Medicine archived successfully.", response));
    }

    @GetMapping("/stock")
    public ResponseEntity<ApiResponse<List<StockBatchRecord>>> stock(
            @RequestParam(required = false) Long medicineId
    ) {
        log.info("Pharmacy stock API called. medicineId={}", medicineId);

        List<StockBatchRecord> response = service.listStock(medicineId);

        log.info("Pharmacy stock API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Pharmacy stock fetched successfully.", response));
    }

    @PostMapping("/stock/batches")
    public ResponseEntity<ApiResponse<StockBatchRecord>> createStockBatch(
            @Valid @RequestBody CreateStockBatchRequest request
    ) {
        log.info("Create stock batch API called.");

        StockBatchRecord response = service.createStockBatch(request);

        log.info("Create stock batch API completed. batchId={}", response.id());

        return ResponseEntity.ok(ApiResponse.success("Stock batch created successfully.", response));
    }

    @GetMapping("/stock/low-stock")
    public ResponseEntity<ApiResponse<List<StockBatchRecord>>> lowStock() {
        log.info("Low stock API called.");

        List<StockBatchRecord> response = service.lowStock();

        log.info("Low stock API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Low stock fetched successfully.", response));
    }

    @GetMapping("/stock/expiring")
    public ResponseEntity<ApiResponse<List<StockBatchRecord>>> expiringStock(
            @RequestParam(required = false) Integer days
    ) {
        log.info("Expiring stock API called. days={}", days);

        List<StockBatchRecord> response = service.expiringStock(days);

        log.info("Expiring stock API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Expiring stock fetched successfully.", response));
    }

    @PostMapping("/stock/adjust")
    public ResponseEntity<ApiResponse<StockBatchRecord>> adjustStock(
            @Valid @RequestBody AdjustStockRequest request
    ) {
        log.info("Stock adjust API called.");

        StockBatchRecord response = service.adjustStock(request);

        log.info("Stock adjust API completed. batchId={}", response.id());

        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully.", response));
    }

    @GetMapping("/stock/movements")
    public ResponseEntity<ApiResponse<List<StockMovementRecord>>> stockMovements(
            @RequestParam(required = false) Long medicineId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        log.info("Stock movements API called.");

        List<StockMovementRecord> response = service.stockMovements(medicineId, batchId, page, size);

        log.info("Stock movements API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Stock movements fetched successfully.", response));
    }
}