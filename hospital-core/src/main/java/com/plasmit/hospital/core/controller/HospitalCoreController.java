package com.plasmit.hospital.core.controller;

import java.util.List;

import com.plasmit.hospital.core.common.ApiResponse;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.BranchOption;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.DepartmentOption;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.ReferenceOption;
import com.plasmit.hospital.core.service.HospitalCoreService;
import com.plasmit.hospital.core.service.HospitalCoreService.SettingsResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HospitalCoreController {

    private static final Logger log = LoggerFactory.getLogger(HospitalCoreController.class);

    private final HospitalCoreService service;

    public HospitalCoreController(HospitalCoreService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/hospital/settings")
    public ResponseEntity<ApiResponse<SettingsResponse>> settings() {

        log.info("Hospital settings API called.");

        SettingsResponse response = service.getSettings();

        log.info("Hospital settings API completed.");

        return ResponseEntity.ok(ApiResponse.success("Hospital settings fetched successfully.", response));
    }

    @GetMapping("/api/v1/hospital/reference/branches")
    public ResponseEntity<ApiResponse<List<BranchOption>>> branches() {

        log.info("Branch reference API called.");

        List<BranchOption> response = service.getBranches();

        log.info("Branch reference API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Branches fetched successfully.", response));
    }

    @GetMapping("/api/v1/hospital/reference/departments")
    public ResponseEntity<ApiResponse<List<DepartmentOption>>> departments() {

        log.info("Department reference API called.");

        List<DepartmentOption> response = service.getDepartments();

        log.info("Department reference API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Departments fetched successfully.", response));
    }

    @GetMapping("/api/v1/hospital/reference/blood-groups")
    public ResponseEntity<ApiResponse<List<ReferenceOption>>> bloodGroups() {

        log.info("Blood group reference API called.");

        List<ReferenceOption> response = service.getBloodGroups();

        log.info("Blood group reference API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Blood groups fetched successfully.", response));
    }

    @GetMapping("/api/v1/hospital/reference/payment-methods")
    public ResponseEntity<ApiResponse<List<ReferenceOption>>> paymentMethods() {

        log.info("Payment method reference API called.");

        List<ReferenceOption> response = service.getPaymentMethods();

        log.info("Payment method reference API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Payment methods fetched successfully.", response));
    }

    @GetMapping("/api/v1/hospital/reference/appointment-types")
    public ResponseEntity<ApiResponse<List<ReferenceOption>>> appointmentTypes() {

        log.info("Appointment type reference API called.");

        List<ReferenceOption> response = service.getAppointmentTypes();

        log.info("Appointment type reference API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Appointment types fetched successfully.", response));
    }

    @GetMapping("/api/v1/hospital/reference/source-channels")
    public ResponseEntity<ApiResponse<List<ReferenceOption>>> sourceChannels() {

        log.info("Source channel reference API called.");

        List<ReferenceOption> response = service.getSourceChannels();

        log.info("Source channel reference API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Source channels fetched successfully.", response));
    }

    @GetMapping("/api/v1/hospital/reference/payer-types")
    public ResponseEntity<ApiResponse<List<ReferenceOption>>> payerTypes() {

        log.info("Payer type reference API called.");

        List<ReferenceOption> response = service.getPayerTypes();

        log.info("Payer type reference API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Payer types fetched successfully.", response));
    }
}