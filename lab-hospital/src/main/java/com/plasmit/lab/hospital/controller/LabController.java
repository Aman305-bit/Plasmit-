package com.plasmit.lab.hospital.controller;

import java.util.List;
import java.util.Map;

import com.plasmit.lab.hospital.common.ApiResponse;
import com.plasmit.lab.hospital.repository.LabRepository.BillingServiceRecord;
import com.plasmit.lab.hospital.repository.LabRepository.PackageRecord;
import com.plasmit.lab.hospital.repository.LabRepository.TestRecord;
import com.plasmit.lab.hospital.service.LabService;
import com.plasmit.lab.hospital.service.LabService.CreatePackageRequest;
import com.plasmit.lab.hospital.service.LabService.CreateTestRequest;
import com.plasmit.lab.hospital.service.LabService.PackageDetailResponse;
import com.plasmit.lab.hospital.service.LabService.UpdatePackageRequest;
import com.plasmit.lab.hospital.service.LabService.UpdateTestRequest;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hospital")
public class LabController {

    private static final Logger log = LoggerFactory.getLogger(LabController.class);

    private final LabService service;

    public LabController(LabService service) {
        this.service = service;
    }

    @GetMapping("/test-catalog")
    public ResponseEntity<ApiResponse<List<TestRecord>>> listTests(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        log.info("Lab test list API called.");

        List<TestRecord> response = service.listTests(query, category, status, page, size);

        log.info("Lab test list API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Lab tests fetched successfully.", response));
    }

    @PostMapping("/test-catalog")
    public ResponseEntity<ApiResponse<TestRecord>> createTest(
            @Valid @RequestBody CreateTestRequest request
    ) {

        log.info("Create lab test API called.");

        TestRecord response = service.createTest(request);

        log.info("Create lab test API completed. testId={}", response.id());

        return ResponseEntity.ok(ApiResponse.success("Lab test created successfully.", response));
    }

    @GetMapping("/test-catalog/{testId}")
    public ResponseEntity<ApiResponse<TestRecord>> testDetail(
            @PathVariable Long testId
    ) {

        log.info("Lab test detail API called. testId={}", testId);

        TestRecord response = service.getTest(testId);

        log.info("Lab test detail API completed. testId={}", testId);

        return ResponseEntity.ok(ApiResponse.success("Lab test fetched successfully.", response));
    }

    @PatchMapping("/test-catalog/{testId}")
    public ResponseEntity<ApiResponse<TestRecord>> updateTest(
            @PathVariable Long testId,
            @Valid @RequestBody UpdateTestRequest request
    ) {

        log.info("Update lab test API called. testId={}", testId);

        TestRecord response = service.updateTest(testId, request);

        log.info("Update lab test API completed. testId={}", testId);

        return ResponseEntity.ok(ApiResponse.success("Lab test updated successfully.", response));
    }

    @DeleteMapping("/test-catalog/{testId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> archiveTest(
            @PathVariable Long testId
    ) {

        log.info("Archive lab test API called. testId={}", testId);

        Map<String, Object> response = service.archiveTest(testId);

        log.info("Archive lab test API completed. testId={}", testId);

        return ResponseEntity.ok(ApiResponse.success("Lab test archived successfully.", response));
    }

    @GetMapping("/test-packages")
    public ResponseEntity<ApiResponse<List<PackageRecord>>> listPackages(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        log.info("Lab package list API called.");

        List<PackageRecord> response = service.listPackages(query, status, page, size);

        log.info("Lab package list API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Lab packages fetched successfully.", response));
    }

    @PostMapping("/test-packages")
    public ResponseEntity<ApiResponse<PackageDetailResponse>> createPackage(
            @Valid @RequestBody CreatePackageRequest request
    ) {

        log.info("Create lab package API called.");

        PackageDetailResponse response = service.createPackage(request);

        log.info("Create lab package API completed. packageId={}", response.packageInfo().id());

        return ResponseEntity.ok(ApiResponse.success("Lab package created successfully.", response));
    }

    @GetMapping("/test-packages/{packageId}")
    public ResponseEntity<ApiResponse<PackageDetailResponse>> packageDetail(
            @PathVariable Long packageId
    ) {

        log.info("Lab package detail API called. packageId={}", packageId);

        PackageDetailResponse response = service.getPackage(packageId);

        log.info("Lab package detail API completed. packageId={}", packageId);

        return ResponseEntity.ok(ApiResponse.success("Lab package fetched successfully.", response));
    }

    @PatchMapping("/test-packages/{packageId}")
    public ResponseEntity<ApiResponse<PackageDetailResponse>> updatePackage(
            @PathVariable Long packageId,
            @Valid @RequestBody UpdatePackageRequest request
    ) {

        log.info("Update lab package API called. packageId={}", packageId);

        PackageDetailResponse response = service.updatePackage(packageId, request);

        log.info("Update lab package API completed. packageId={}", packageId);

        return ResponseEntity.ok(ApiResponse.success("Lab package updated successfully.", response));
    }

    @DeleteMapping("/test-packages/{packageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> archivePackage(
            @PathVariable Long packageId
    ) {

        log.info("Archive lab package API called. packageId={}", packageId);

        Map<String, Object> response = service.archivePackage(packageId);

        log.info("Archive lab package API completed. packageId={}", packageId);

        return ResponseEntity.ok(ApiResponse.success("Lab package archived successfully.", response));
    }

    @GetMapping("/billing/services")
    public ResponseEntity<ApiResponse<List<BillingServiceRecord>>> billingServices(
            @RequestParam(required = false) String query
    ) {

        log.info("Billing services API called. query={}", query);

        List<BillingServiceRecord> response = service.billingServices(query);

        log.info("Billing services API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Billing services fetched successfully.", response));
    }
}