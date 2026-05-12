package com.plasmit.prescription.hospital.controller;

import java.util.List;
import java.util.Map;

import com.plasmit.prescription.hospital.common.ApiResponse;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository.PrescriptionRecord;
import com.plasmit.prescription.hospital.service.PrescriptionService;
import com.plasmit.prescription.hospital.service.PrescriptionService.CreatePrescriptionRequest;
import com.plasmit.prescription.hospital.service.PrescriptionService.PrescriptionDetailResponse;
import com.plasmit.prescription.hospital.service.PrescriptionService.UpdatePrescriptionRequest;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hospital/prescriptions")
public class PrescriptionController {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionController.class);

    private final PrescriptionService service;

    public PrescriptionController(PrescriptionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PrescriptionRecord>>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long appointmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        log.info("Prescription list API called.");

        List<PrescriptionRecord> response = service.listPrescriptions(
                patientId,
                doctorId,
                appointmentId,
                status,
                page,
                size
        );

        log.info("Prescription list API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Prescriptions fetched successfully.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PrescriptionDetailResponse>> create(
            @Valid @RequestBody CreatePrescriptionRequest request
    ) {
        log.info("Create prescription API called.");

        PrescriptionDetailResponse response = service.createPrescription(request);

        log.info("Create prescription API completed. prescriptionId={}", response.prescription().id());

        return ResponseEntity.ok(ApiResponse.success("Prescription created successfully.", response));
    }

    @GetMapping("/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionDetailResponse>> detail(
            @PathVariable Long prescriptionId
    ) {
        log.info("Prescription detail API called. prescriptionId={}", prescriptionId);

        PrescriptionDetailResponse response = service.getPrescription(prescriptionId);

        log.info("Prescription detail API completed. prescriptionId={}", prescriptionId);

        return ResponseEntity.ok(ApiResponse.success("Prescription fetched successfully.", response));
    }

    @PatchMapping("/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionDetailResponse>> update(
            @PathVariable Long prescriptionId,
            @Valid @RequestBody UpdatePrescriptionRequest request
    ) {
        log.info("Update prescription API called. prescriptionId={}", prescriptionId);

        PrescriptionDetailResponse response = service.updatePrescription(prescriptionId, request);

        log.info("Update prescription API completed. prescriptionId={}", prescriptionId);

        return ResponseEntity.ok(ApiResponse.success("Prescription updated successfully.", response));
    }

    @PostMapping("/{prescriptionId}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancel(
            @PathVariable Long prescriptionId
    ) {
        log.info("Cancel prescription API called. prescriptionId={}", prescriptionId);

        Map<String, Object> response = service.cancelPrescription(prescriptionId);

        log.info("Cancel prescription API completed. prescriptionId={}", prescriptionId);

        return ResponseEntity.ok(ApiResponse.success("Prescription cancelled successfully.", response));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PrescriptionRecord>>> patientHistory(
            @PathVariable Long patientId
    ) {
        log.info("Patient prescription history API called. patientId={}", patientId);

        List<PrescriptionRecord> response = service.patientHistory(patientId);

        log.info("Patient prescription history API completed. patientId={} count={}",
                patientId,
                response.size());

        return ResponseEntity.ok(ApiResponse.success("Patient prescription history fetched successfully.", response));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ApiResponse<List<PrescriptionRecord>>> appointmentPrescriptions(
            @PathVariable Long appointmentId
    ) {
        log.info("Appointment prescriptions API called. appointmentId={}", appointmentId);

        List<PrescriptionRecord> response = service.appointmentPrescriptions(appointmentId);

        log.info("Appointment prescriptions API completed. appointmentId={} count={}",
                appointmentId,
                response.size());

        return ResponseEntity.ok(ApiResponse.success("Appointment prescriptions fetched successfully.", response));
    }
}