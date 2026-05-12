package com.plasmit.doctor.hospital.controller;

import java.util.List;
import java.util.Map;

import com.plasmit.doctor.hospital.common.ApiResponse;
import com.plasmit.doctor.hospital.repository.DoctorRepository.AvailabilityRecord;
import com.plasmit.doctor.hospital.repository.DoctorRepository.DoctorRecord;
import com.plasmit.doctor.hospital.service.DoctorService;
import com.plasmit.doctor.hospital.service.DoctorService.CreateDoctorRequest;
import com.plasmit.doctor.hospital.service.DoctorService.UpdateAvailabilityRequest;
import com.plasmit.doctor.hospital.service.DoctorService.UpdateDoctorRequest;
import com.plasmit.doctor.hospital.service.DoctorService.UpdateFeeRequest;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hospital/doctors")
public class DoctorController {

    private static final Logger log = LoggerFactory.getLogger(DoctorController.class);

    private final DoctorService service;

    public DoctorController(DoctorService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorRecord>>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        log.info("Doctor list API called.");

        List<DoctorRecord> response = service.listDoctors(query, status, page, size);

        log.info("Doctor list API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Doctors fetched successfully.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorRecord>> create(
            @Valid @RequestBody CreateDoctorRequest request
    ) {

        log.info("Create doctor API called.");

        DoctorRecord response = service.createDoctor(request);

        log.info("Create doctor API completed. doctorId={}", response.id());

        return ResponseEntity.ok(ApiResponse.success("Doctor created successfully.", response));
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorRecord>> detail(
            @PathVariable Long doctorId
    ) {

        log.info("Doctor detail API called. doctorId={}", doctorId);

        DoctorRecord response = service.getDoctor(doctorId);

        log.info("Doctor detail API completed. doctorId={}", doctorId);

        return ResponseEntity.ok(ApiResponse.success("Doctor fetched successfully.", response));
    }

    @PatchMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorRecord>> update(
            @PathVariable Long doctorId,
            @Valid @RequestBody UpdateDoctorRequest request
    ) {

        log.info("Update doctor API called. doctorId={}", doctorId);

        DoctorRecord response = service.updateDoctor(doctorId, request);

        log.info("Update doctor API completed. doctorId={}", doctorId);

        return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully.", response));
    }

    @DeleteMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(
            @PathVariable Long doctorId
    ) {

        log.info("Archive doctor API called. doctorId={}", doctorId);

        Map<String, Object> response = service.deleteDoctor(doctorId);

        log.info("Archive doctor API completed. doctorId={}", doctorId);

        return ResponseEntity.ok(ApiResponse.success("Doctor archived successfully.", response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DoctorRecord>>> search(
            @RequestParam String query
    ) {

        log.info("Doctor search API called. query={}", query);

        List<DoctorRecord> response = service.search(query);

        log.info("Doctor search API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Doctor search completed successfully.", response));
    }

    @GetMapping("/{doctorId}/availability")
    public ResponseEntity<ApiResponse<List<AvailabilityRecord>>> availability(
            @PathVariable Long doctorId
    ) {

        log.info("Doctor availability API called. doctorId={}", doctorId);

        List<AvailabilityRecord> response = service.getAvailability(doctorId);

        log.info("Doctor availability API completed. doctorId={} count={}", doctorId, response.size());

        return ResponseEntity.ok(ApiResponse.success("Doctor availability fetched successfully.", response));
    }

    @PatchMapping("/{doctorId}/availability")
    public ResponseEntity<ApiResponse<List<AvailabilityRecord>>> updateAvailability(
            @PathVariable Long doctorId,
            @Valid @RequestBody UpdateAvailabilityRequest request
    ) {

        log.info("Update doctor availability API called. doctorId={}", doctorId);

        List<AvailabilityRecord> response = service.updateAvailability(doctorId, request);

        log.info("Update doctor availability API completed. doctorId={} count={}", doctorId, response.size());

        return ResponseEntity.ok(ApiResponse.success("Doctor availability updated successfully.", response));
    }

    @PatchMapping("/{doctorId}/consultation-fee")
    public ResponseEntity<ApiResponse<DoctorRecord>> updateFee(
            @PathVariable Long doctorId,
            @Valid @RequestBody UpdateFeeRequest request
    ) {

        log.info("Update doctor consultation fee API called. doctorId={}", doctorId);

        DoctorRecord response = service.updateConsultationFee(doctorId, request);

        log.info("Update doctor consultation fee API completed. doctorId={}", doctorId);

        return ResponseEntity.ok(ApiResponse.success("Doctor consultation fee updated successfully.", response));
    }
}