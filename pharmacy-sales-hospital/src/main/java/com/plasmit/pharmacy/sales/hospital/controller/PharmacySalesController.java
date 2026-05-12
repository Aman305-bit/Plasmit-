package com.plasmit.pharmacy.sales.hospital.controller;

import java.util.List;
import java.util.Map;

import com.plasmit.pharmacy.sales.hospital.common.ApiResponse;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.SaleRecord;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.CancelRequest;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.CreateSaleFromPrescriptionRequest;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.CreateSaleRequest;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.PaymentRequest;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.SaleDetailResponse;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hospital/pharmacy-sales")
public class PharmacySalesController {

    private static final Logger log = LoggerFactory.getLogger(PharmacySalesController.class);

    private final PharmacySalesService service;

    public PharmacySalesController(PharmacySalesService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleRecord>>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long prescriptionId,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String saleStatus,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        log.info("Pharmacy sale list API called.");

        List<SaleRecord> response = service.listSales(
                patientId,
                prescriptionId,
                paymentStatus,
                saleStatus,
                page,
                size
        );

        log.info("Pharmacy sale list API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Pharmacy sales fetched successfully.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleDetailResponse>> createSale(
            @Valid @RequestBody CreateSaleRequest request
    ) {
        log.info("Create pharmacy sale API called.");

        SaleDetailResponse response = service.createSale(request);

        log.info("Create pharmacy sale API completed. saleId={}", response.sale().id());

        return ResponseEntity.ok(ApiResponse.success("Pharmacy sale created successfully.", response));
    }

    @PostMapping("/from-prescription")
    public ResponseEntity<ApiResponse<SaleDetailResponse>> createFromPrescription(
            @Valid @RequestBody CreateSaleFromPrescriptionRequest request
    ) {
        log.info("Create pharmacy sale from prescription API called. prescriptionId={}", request.prescriptionId());

        SaleDetailResponse response = service.createSaleFromPrescription(request);

        log.info("Create pharmacy sale from prescription API completed. saleId={}", response.sale().id());

        return ResponseEntity.ok(ApiResponse.success("Pharmacy sale created from prescription successfully.", response));
    }

    @GetMapping("/{saleId}")
    public ResponseEntity<ApiResponse<SaleDetailResponse>> detail(
            @PathVariable Long saleId
    ) {
        log.info("Pharmacy sale detail API called. saleId={}", saleId);

        SaleDetailResponse response = service.getSale(saleId);

        log.info("Pharmacy sale detail API completed. saleId={}", saleId);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy sale fetched successfully.", response));
    }

    @PostMapping("/{saleId}/payments")
    public ResponseEntity<ApiResponse<SaleDetailResponse>> addPayment(
            @PathVariable Long saleId,
            @Valid @RequestBody PaymentRequest request
    ) {
        log.info("Pharmacy sale payment API called. saleId={}", saleId);

        SaleDetailResponse response = service.addPayment(saleId, request);

        log.info("Pharmacy sale payment API completed. saleId={}", saleId);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy payment added successfully.", response));
    }

    @PostMapping("/{saleId}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelSale(
            @PathVariable Long saleId,
            @RequestBody(required = false) CancelRequest request
    ) {
        log.info("Pharmacy sale cancel API called. saleId={}", saleId);

        Map<String, Object> response = service.cancelSale(saleId, request);

        log.info("Pharmacy sale cancel API completed. saleId={}", saleId);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy sale cancelled successfully.", response));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<SaleRecord>>> patientSales(
            @PathVariable Long patientId
    ) {
        log.info("Patient pharmacy sales API called. patientId={}", patientId);

        List<SaleRecord> response = service.listSales(patientId, null, null, null, 1, 100);

        log.info("Patient pharmacy sales API completed. patientId={} count={}", patientId, response.size());

        return ResponseEntity.ok(ApiResponse.success("Patient pharmacy sales fetched successfully.", response));
    }

    @GetMapping("/prescription/{prescriptionId}")
    public ResponseEntity<ApiResponse<List<SaleRecord>>> prescriptionSales(
            @PathVariable Long prescriptionId
    ) {
        log.info("Prescription pharmacy sales API called. prescriptionId={}", prescriptionId);

        List<SaleRecord> response = service.listSales(null, prescriptionId, null, null, 1, 100);

        log.info("Prescription pharmacy sales API completed. prescriptionId={} count={}", prescriptionId, response.size());

        return ResponseEntity.ok(ApiResponse.success("Prescription pharmacy sales fetched successfully.", response));
    }
}