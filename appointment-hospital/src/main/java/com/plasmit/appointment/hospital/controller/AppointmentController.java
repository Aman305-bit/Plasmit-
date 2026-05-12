package com.plasmit.appointment.hospital.controller;

import java.time.LocalDate;
import java.util.List;

import com.plasmit.appointment.hospital.common.ApiResponse;
import com.plasmit.appointment.hospital.repository.AppointmentRepository.AppointmentRecord;
import com.plasmit.appointment.hospital.service.AppointmentService;
import com.plasmit.appointment.hospital.service.AppointmentService.AvailabilityResponse;
import com.plasmit.appointment.hospital.service.AppointmentService.CreateAppointmentRequest;
import com.plasmit.appointment.hospital.service.AppointmentService.RescheduleRequest;
import com.plasmit.appointment.hospital.service.AppointmentService.StatusActionRequest;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hospital/appointments")
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentRecord>>> list(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {

        log.info("Appointment list API called.");

        List<AppointmentRecord> response = service.listAppointments(date, status, doctorId, patientId, page, size);

        log.info("Appointment list API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Appointments fetched successfully.", response));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<AppointmentRecord>>> today() {

        log.info("Today appointments API called.");

        List<AppointmentRecord> response = service.todayAppointments();

        log.info("Today appointments API completed. count={}", response.size());

        return ResponseEntity.ok(ApiResponse.success("Today appointments fetched successfully.", response));
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<ApiResponse<AppointmentRecord>> detail(
            @PathVariable Long appointmentId
    ) {

        log.info("Appointment detail API called. appointmentId={}", appointmentId);

        AppointmentRecord response = service.getAppointment(appointmentId);

        log.info("Appointment detail API completed. appointmentId={}", appointmentId);

        return ResponseEntity.ok(ApiResponse.success("Appointment fetched successfully.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentRecord>> create(
            @Valid @RequestBody CreateAppointmentRequest request
    ) {

        log.info("Create appointment API called.");

        AppointmentRecord response = service.createAppointment(request);

        log.info("Create appointment API completed. appointmentId={}", response.id());

        return ResponseEntity.ok(ApiResponse.success("Appointment created successfully.", response));
    }

    @PostMapping("/{appointmentId}/check-in")
    public ResponseEntity<ApiResponse<AppointmentRecord>> checkIn(
            @PathVariable Long appointmentId,
            @RequestBody(required = false) StatusActionRequest request
    ) {

        log.info("Appointment check-in API called. appointmentId={}", appointmentId);

        AppointmentRecord response = service.checkIn(appointmentId, request);

        log.info("Appointment check-in API completed. appointmentId={}", appointmentId);

        return ResponseEntity.ok(ApiResponse.success("Appointment checked-in successfully.", response));
    }

    @PostMapping("/{appointmentId}/complete")
    public ResponseEntity<ApiResponse<AppointmentRecord>> complete(
            @PathVariable Long appointmentId,
            @RequestBody(required = false) StatusActionRequest request
    ) {

        log.info("Appointment complete API called. appointmentId={}", appointmentId);

        AppointmentRecord response = service.complete(appointmentId, request);

        log.info("Appointment complete API completed. appointmentId={}", appointmentId);

        return ResponseEntity.ok(ApiResponse.success("Appointment completed successfully.", response));
    }

    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<AppointmentRecord>> cancel(
            @PathVariable Long appointmentId,
            @RequestBody(required = false) StatusActionRequest request
    ) {

        log.info("Appointment cancel API called. appointmentId={}", appointmentId);

        AppointmentRecord response = service.cancel(appointmentId, request);

        log.info("Appointment cancel API completed. appointmentId={}", appointmentId);

        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully.", response));
    }

    @PostMapping("/{appointmentId}/reschedule")
    public ResponseEntity<ApiResponse<AppointmentRecord>> reschedule(
            @PathVariable Long appointmentId,
            @Valid @RequestBody RescheduleRequest request
    ) {

        log.info("Appointment reschedule API called. appointmentId={}", appointmentId);

        AppointmentRecord response = service.reschedule(appointmentId, request);

        log.info("Appointment reschedule API completed. appointmentId={}", appointmentId);

        return ResponseEntity.ok(ApiResponse.success("Appointment rescheduled successfully.", response));
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> availability(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date
    ) {

        log.info("Appointment availability API called. doctorId={} date={}", doctorId, date);

        AvailabilityResponse response = service.availability(doctorId, date);

        log.info("Appointment availability API completed. doctorId={} date={}", doctorId, date);

        return ResponseEntity.ok(ApiResponse.success("Appointment availability fetched successfully.", response));
    }
}