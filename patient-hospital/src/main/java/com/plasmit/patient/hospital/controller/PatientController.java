package com.plasmit.patient.hospital.controller;

import java.util.List;
import java.util.Map;

import com.plasmit.patient.hospital.common.ApiResponse;
import com.plasmit.patient.hospital.repository.PatientRepository.PatientRecord;
import com.plasmit.patient.hospital.service.PatientService;
import com.plasmit.patient.hospital.service.PatientService.CreatePatientRequest;
import com.plasmit.patient.hospital.service.PatientService.DuplicateCheckResponse;
import com.plasmit.patient.hospital.service.PatientService.UpdatePatientRequest;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hospital/patients")
public class PatientController {

    private static final Logger log = LoggerFactory.getLogger(PatientController.class);

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientRecord>>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        log.info("Patient list API called.");

        List<PatientRecord> response = service.listPatients(query, status, page, size);

        log.info("Patient list API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Patients fetched successfully.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientRecord>> create(
            @Valid @RequestBody CreatePatientRequest request
    ) {

        log.info("Create patient API called.");

        PatientRecord response = service.createPatient(request);

        log.info("Create patient API completed. patientId={}", response.id());

        return ResponseEntity.ok(ApiResponse.success("Patient created successfully.", response));
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<PatientRecord>> detail(
            @PathVariable Long patientId
    ) {

        log.info("Patient detail API called. patientId={}", patientId);

        PatientRecord response = service.getPatient(patientId);

        log.info("Patient detail API completed. patientId={}", patientId);

        return ResponseEntity.ok(ApiResponse.success("Patient fetched successfully.", response));
    }

    @PatchMapping("/{patientId}")
    public ResponseEntity<ApiResponse<PatientRecord>> update(
            @PathVariable Long patientId,
            @Valid @RequestBody UpdatePatientRequest request
    ) {

        log.info("Update patient API called. patientId={}", patientId);

        PatientRecord response = service.updatePatient(patientId, request);

        log.info("Update patient API completed. patientId={}", patientId);

        return ResponseEntity.ok(ApiResponse.success("Patient updated successfully.", response));
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(
            @PathVariable Long patientId
    ) {

        log.info("Delete patient API called. patientId={}", patientId);

        Map<String, Object> response = service.deletePatient(patientId);

        log.info("Delete patient API completed. patientId={}", patientId);

        return ResponseEntity.ok(ApiResponse.success("Patient deleted successfully.", response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PatientRecord>>> search(
            @RequestParam String query
    ) {

        log.info("Patient search API called. query={}", query);

        List<PatientRecord> response = service.search(query);

        log.info("Patient search API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Patient search completed successfully.", response));
    }

    @GetMapping("/duplicate-check")
    public ResponseEntity<ApiResponse<DuplicateCheckResponse>> duplicateCheck(
            @RequestParam String phone
    ) {

        log.info("Patient duplicate check API called. phone={}", phone);

        DuplicateCheckResponse response = service.duplicateCheck(phone);

        log.info("Patient duplicate check API completed. exists={}", response.exists());

        return ResponseEntity.ok(ApiResponse.success("Duplicate check completed successfully.", response));
    }
}