package com.plasmit.appointment.hospital.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.plasmit.appointment.hospital.exception.ApiException;
import com.plasmit.appointment.hospital.repository.AppointmentRepository;
import com.plasmit.appointment.hospital.repository.AppointmentRepository.AppointmentRecord;
import com.plasmit.appointment.hospital.repository.AppointmentRepository.SlotResponse;
import com.plasmit.appointment.hospital.security.SecurityConfig.CurrentUser;
import com.plasmit.appointment.hospital.security.SecurityConfig.TenantContext;
import com.plasmit.appointment.hospital.validator.AppointmentValidator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository repository;
    private final AppointmentValidator validator;

    public AppointmentService(
            AppointmentRepository repository,
            AppointmentValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<AppointmentRecord> listAppointments(
            LocalDate date,
            String status,
            Long doctorId,
            Long patientId,
            Integer page,
            Integer size
    ) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateStatusFilter(status);

        if (doctorId != null) {
            validator.validateDoctorId(doctorId);
        }

        if (patientId != null) {
            validator.validatePatientId(patientId);
        }

        requirePermission(currentUser, "appointments.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        log.info("List appointments request. userId={} tenantId={} hospitalId={} branchId={} date={} status={}",
                currentUser.getUserId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                date,
                status);

        List<AppointmentRecord> appointments = repository.findAppointments(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                date,
                status,
                doctorId,
                patientId,
                safeSize,
                offset
        );

        validator.validateAppointmentList(
                appointments,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return appointments;
    }

    public List<AppointmentRecord> todayAppointments() {
        return listAppointments(LocalDate.now(), null, null, null, 1, 100);
    }

    public AppointmentRecord getAppointment(Long appointmentId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateAppointmentId(appointmentId);

        requirePermission(currentUser, "appointments.view");

        log.info("Appointment detail request. appointmentId={} tenantId={} hospitalId={}",
                appointmentId,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        AppointmentRecord appointment = repository.findById(
                appointmentId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateAppointmentFound(
                appointment,
                appointmentId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return appointment;
    }

    @Transactional
    public AppointmentRecord createAppointment(CreateAppointmentRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateRequest(request);

        requirePermission(currentUser, "appointments.create");

        log.info("Create appointment request. tenantId={} hospitalId={} patientId={} doctorId={} date={} time={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.patientId(),
                request.doctorId(),
                request.appointmentDate(),
                request.appointmentTime());

        validatePatientDoctorAndSlot(
                currentUser,
                request.patientId(),
                request.doctorId(),
                request.appointmentDate(),
                request.appointmentTime(),
                null
        );

        String appointmentCode = repository.generateNextAppointmentCode(currentUser.getHospitalId());
        LocalTime endTime = request.appointmentTime().plusMinutes(15);

        Long appointmentId = repository.createAppointment(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getDepartmentId(),
                currentUser.getUserId(),
                appointmentCode,
                request,
                endTime
        );

        repository.saveStatusHistory(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                appointmentId,
                null,
                "BOOKED",
                "Appointment created.",
                currentUser.getUserId()
        );

        log.info("Appointment created successfully. appointmentId={} appointmentCode={}",
                appointmentId,
                appointmentCode);

        return getAppointment(appointmentId);
    }

    @Transactional
    public AppointmentRecord checkIn(Long appointmentId, StatusActionRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateAppointmentId(appointmentId);

        requirePermission(currentUser, "appointments.checkin");

        AppointmentRecord appointment = getAppointment(appointmentId);

        validator.validateCheckInAllowed(appointment);

        updateStatusInternal(currentUser, appointment, "CHECKED_IN", safeRemarks(request));

        log.info("Appointment checked-in successfully. appointmentId={}", appointmentId);

        return getAppointment(appointmentId);
    }

    @Transactional
    public AppointmentRecord complete(Long appointmentId, StatusActionRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateAppointmentId(appointmentId);

        requirePermission(currentUser, "appointments.update");

        AppointmentRecord appointment = getAppointment(appointmentId);

        validator.validateCompleteAllowed(appointment);

        updateStatusInternal(currentUser, appointment, "COMPLETED", safeRemarks(request));

        log.info("Appointment completed successfully. appointmentId={}", appointmentId);

        return getAppointment(appointmentId);
    }

    @Transactional
    public AppointmentRecord cancel(Long appointmentId, StatusActionRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateAppointmentId(appointmentId);

        requirePermission(currentUser, "appointments.cancel");

        AppointmentRecord appointment = getAppointment(appointmentId);

        validator.validateCancelAllowed(appointment);

        updateStatusInternal(currentUser, appointment, "CANCELLED", safeRemarks(request));

        log.info("Appointment cancelled successfully. appointmentId={}", appointmentId);

        return getAppointment(appointmentId);
    }

    @Transactional
    public AppointmentRecord reschedule(Long appointmentId, RescheduleRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateAppointmentId(appointmentId);
        validator.validateRescheduleRequest(request);

        requirePermission(currentUser, "appointments.update");

        AppointmentRecord appointment = getAppointment(appointmentId);

        validator.validateRescheduleAllowed(appointment);

        validatePatientDoctorAndSlot(
                currentUser,
                appointment.patientId(),
                appointment.doctorId(),
                request.appointmentDate(),
                request.appointmentTime(),
                appointmentId
        );

        LocalTime endTime = request.appointmentTime().plusMinutes(15);

        int updated = repository.reschedule(
                appointmentId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request,
                endTime
        );

        validator.validateUpdateCount(
                updated,
                appointmentId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        repository.saveStatusHistory(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                appointmentId,
                appointment.status(),
                "BOOKED",
                "Appointment rescheduled.",
                currentUser.getUserId()
        );

        log.info("Appointment rescheduled successfully. appointmentId={} date={} time={}",
                appointmentId,
                request.appointmentDate(),
                request.appointmentTime());

        return getAppointment(appointmentId);
    }

    public AvailabilityResponse availability(Long doctorId, LocalDate date) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateDoctorId(doctorId);
        validator.validateAppointmentDate(date);

        requirePermission(currentUser, "appointments.view");

        boolean doctorExists = repository.doctorExists(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                doctorId
        );

        validator.validateDoctorExists(
                doctorExists,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<SlotResponse> bookedSlots = repository.findBookedSlots(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                doctorId,
                date
        );

        validator.validateBookedSlots(bookedSlots, doctorId, date);

        return new AvailabilityResponse(
                doctorId,
                date,
                dayName(date),
                bookedSlots
        );
    }

    private void validatePatientDoctorAndSlot(
            CurrentUser currentUser,
            Long patientId,
            Long doctorId,
            LocalDate date,
            LocalTime time,
            Long excludeAppointmentId
    ) {

        validator.validatePatientId(patientId);
        validator.validateDoctorId(doctorId);
        validator.validateAppointmentDate(date);
        validator.validateAppointmentTime(time);

        boolean patientExists = repository.patientExists(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                patientId
        );

        validator.validatePatientExists(
                patientExists,
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        boolean doctorExists = repository.doctorExists(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                doctorId
        );

        validator.validateDoctorExists(
                doctorExists,
                doctorId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        String dayOfWeek = dayName(date);

        boolean available = repository.doctorAvailable(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                doctorId,
                dayOfWeek,
                time
        );

        validator.validateDoctorAvailable(
                available,
                doctorId,
                date,
                time
        );

        boolean booked = repository.slotAlreadyBooked(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                doctorId,
                date,
                time,
                excludeAppointmentId
        );

        validator.validateSlotNotBooked(
                booked,
                doctorId,
                date,
                time
        );
    }

    private void updateStatusInternal(
            CurrentUser currentUser,
            AppointmentRecord appointment,
            String newStatus,
            String remarks
    ) {

        int updated = repository.updateStatus(
                appointment.id(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                appointment.status(),
                newStatus,
                remarks
        );

        validator.validateUpdateCount(
                updated,
                appointment.id(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        repository.saveStatusHistory(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                appointment.id(),
                appointment.status(),
                newStatus,
                remarks,
                currentUser.getUserId()
        );
    }

    private String dayName(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day.name();
    }

    private String safeRemarks(StatusActionRequest request) {
        if (request == null || request.remarks() == null || request.remarks().isBlank()) {
            return "Status updated.";
        }
        return request.remarks();
    }

    private CurrentUser requireUser() {

        CurrentUser currentUser = TenantContext.get();

        if (currentUser == null) {
            throw ApiException.unauthorized("Authentication required.");
        }

        return currentUser;
    }

    private void requirePermission(CurrentUser currentUser, String permission) {

        if (!currentUser.hasPermission(permission)) {
            log.warn("Permission denied. userId={} tenantId={} hospitalId={} permission={}",
                    currentUser.getUserId(),
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    permission);

            throw ApiException.forbidden("Permission denied.");
        }
    }

    public record CreateAppointmentRequest(
            @NotNull(message = "Patient id is required.")
            Long patientId,

            @NotNull(message = "Doctor id is required.")
            Long doctorId,

            @NotNull(message = "Appointment date is required.")
            LocalDate appointmentDate,

            @NotNull(message = "Appointment time is required.")
            LocalTime appointmentTime,

            String appointmentType,
            String sourceChannel,
            String payerType,

            @NotBlank(message = "Reason is required.")
            String reason,

            String notes
    ) {
    }

    public record RescheduleRequest(
            @NotNull(message = "Appointment date is required.")
            LocalDate appointmentDate,

            @NotNull(message = "Appointment time is required.")
            LocalTime appointmentTime
    ) {
    }

    public record StatusActionRequest(
            String remarks
    ) {
    }

    public record AvailabilityResponse(
            Long doctorId,
            LocalDate date,
            String dayOfWeek,
            List<SlotResponse> bookedSlots
    ) {
    }
}