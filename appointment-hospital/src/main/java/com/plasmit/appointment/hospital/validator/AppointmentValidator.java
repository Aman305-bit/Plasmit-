package com.plasmit.appointment.hospital.validator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.plasmit.appointment.hospital.exception.ApiException;
import com.plasmit.appointment.hospital.repository.AppointmentRepository.AppointmentRecord;
import com.plasmit.appointment.hospital.repository.AppointmentRepository.SlotResponse;
import com.plasmit.appointment.hospital.service.AppointmentService.CreateAppointmentRequest;
import com.plasmit.appointment.hospital.service.AppointmentService.RescheduleRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AppointmentValidator {

    private static final Logger log = LoggerFactory.getLogger(AppointmentValidator.class);

    public void validateTenantHospitalContext(Long tenantId, Long hospitalId) {
        if (tenantId == null || hospitalId == null) {
            log.warn("Appointment validation failed. reason=TENANT_OR_HOSPITAL_MISSING tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (tenantId <= 0 || hospitalId <= 0) {
            log.warn("Appointment validation failed. reason=INVALID_TENANT_OR_HOSPITAL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateBranchContext(Long branchId) {
        if (branchId != null && branchId <= 0) {
            log.warn("Appointment validation failed. reason=INVALID_BRANCH_ID branchId={}", branchId);
            throw ApiException.badRequest("Invalid branch id.");
        }
    }

    public int validatePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public int validateSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }

        if (size > 100) {
            log.warn("Appointment validation warning. reason=PAGE_SIZE_LIMIT_EXCEEDED requestedSize={}", size);
            return 100;
        }

        return size;
    }

    public void validateStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return;
        }

        if (!List.of("BOOKED", "CHECKED_IN", "COMPLETED", "CANCELLED").contains(status.toUpperCase())) {
            log.warn("Appointment validation failed. reason=INVALID_STATUS status={}", status);
            throw ApiException.badRequest("Status must be BOOKED, CHECKED_IN, COMPLETED or CANCELLED.");
        }
    }

    public void validateAppointmentId(Long appointmentId) {
        if (appointmentId == null || appointmentId <= 0) {
            log.warn("Appointment validation failed. reason=INVALID_APPOINTMENT_ID appointmentId={}", appointmentId);
            throw ApiException.badRequest("Invalid appointment id.");
        }
    }

    public void validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            log.warn("Appointment validation failed. reason=INVALID_PATIENT_ID patientId={}", patientId);
            throw ApiException.badRequest("Invalid patient id.");
        }
    }

    public void validateDoctorId(Long doctorId) {
        if (doctorId == null || doctorId <= 0) {
            log.warn("Appointment validation failed. reason=INVALID_DOCTOR_ID doctorId={}", doctorId);
            throw ApiException.badRequest("Invalid doctor id.");
        }
    }

    public void validateAppointmentDate(LocalDate date) {
        if (date == null) {
            log.warn("Appointment validation failed. reason=APPOINTMENT_DATE_REQUIRED");
            throw ApiException.badRequest("Appointment date is required.");
        }

        if (date.isBefore(LocalDate.now())) {
            log.warn("Appointment validation failed. reason=PAST_APPOINTMENT_DATE date={}", date);
            throw ApiException.badRequest("Appointment date cannot be in the past.");
        }
    }

    public void validateAppointmentTime(LocalTime time) {
        if (time == null) {
            log.warn("Appointment validation failed. reason=APPOINTMENT_TIME_REQUIRED");
            throw ApiException.badRequest("Appointment time is required.");
        }
    }

    public void validateCreateRequest(CreateAppointmentRequest request) {
        if (request == null) {
            log.warn("Appointment validation failed. reason=CREATE_REQUEST_NULL");
            throw ApiException.badRequest("Appointment request is required.");
        }

        validatePatientId(request.patientId());
        validateDoctorId(request.doctorId());
        validateAppointmentDate(request.appointmentDate());
        validateAppointmentTime(request.appointmentTime());

        if (request.reason() == null || request.reason().isBlank()) {
            log.warn("Appointment validation failed. reason=REASON_REQUIRED");
            throw ApiException.badRequest("Reason is required.");
        }
    }

    public void validateRescheduleRequest(RescheduleRequest request) {
        if (request == null) {
            log.warn("Appointment validation failed. reason=RESCHEDULE_REQUEST_NULL");
            throw ApiException.badRequest("Reschedule request is required.");
        }

        validateAppointmentDate(request.appointmentDate());
        validateAppointmentTime(request.appointmentTime());
    }

    public void validateAppointmentFound(
            AppointmentRecord appointment,
            Long appointmentId,
            Long tenantId,
            Long hospitalId
    ) {
        if (appointment == null) {
            log.warn("Appointment validation failed. reason=APPOINTMENT_NOT_FOUND appointmentId={} tenantId={} hospitalId={}",
                    appointmentId, tenantId, hospitalId);
            throw ApiException.notFound("Appointment not found.");
        }
    }

    public void validateAppointmentList(
            List<AppointmentRecord> appointments,
            Long tenantId,
            Long hospitalId
    ) {
        if (appointments == null) {
            log.warn("Appointment validation failed. reason=APPOINTMENT_LIST_NULL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.badRequest("Unable to fetch appointments.");
        }
    }

    public void validatePatientExists(boolean exists, Long patientId, Long tenantId, Long hospitalId) {
        if (!exists) {
            log.warn("Appointment validation failed. reason=PATIENT_NOT_FOUND patientId={} tenantId={} hospitalId={}",
                    patientId, tenantId, hospitalId);
            throw ApiException.notFound("Patient not found.");
        }
    }

    public void validateDoctorExists(boolean exists, Long doctorId, Long tenantId, Long hospitalId) {
        if (!exists) {
            log.warn("Appointment validation failed. reason=DOCTOR_NOT_FOUND doctorId={} tenantId={} hospitalId={}",
                    doctorId, tenantId, hospitalId);
            throw ApiException.notFound("Doctor not found.");
        }
    }

    public void validateDoctorAvailable(boolean available, Long doctorId, LocalDate date, LocalTime time) {
        if (!available) {
            log.warn("Appointment validation failed. reason=DOCTOR_NOT_AVAILABLE doctorId={} date={} time={}",
                    doctorId, date, time);
            throw ApiException.conflict("Doctor is not available at selected time.");
        }
    }

    public void validateSlotNotBooked(boolean booked, Long doctorId, LocalDate date, LocalTime time) {
        if (booked) {
            log.warn("Appointment validation failed. reason=SLOT_ALREADY_BOOKED doctorId={} date={} time={}",
                    doctorId, date, time);
            throw ApiException.conflict("Selected appointment slot is already booked.");
        }
    }

    public void validateCheckInAllowed(AppointmentRecord appointment) {
        if (!"BOOKED".equalsIgnoreCase(appointment.status())) {
            log.warn("Appointment validation failed. reason=CHECKIN_NOT_ALLOWED appointmentId={} status={}",
                    appointment.id(), appointment.status());
            throw ApiException.badRequest("Only BOOKED appointment can be checked-in.");
        }
    }

    public void validateCompleteAllowed(AppointmentRecord appointment) {
        if (!"CHECKED_IN".equalsIgnoreCase(appointment.status())) {
            log.warn("Appointment validation failed. reason=COMPLETE_NOT_ALLOWED appointmentId={} status={}",
                    appointment.id(), appointment.status());
            throw ApiException.badRequest("Only CHECKED_IN appointment can be completed.");
        }
    }

    public void validateCancelAllowed(AppointmentRecord appointment) {
        if ("COMPLETED".equalsIgnoreCase(appointment.status())) {
            log.warn("Appointment validation failed. reason=COMPLETED_CANNOT_CANCEL appointmentId={}", appointment.id());
            throw ApiException.badRequest("Completed appointment cannot be cancelled.");
        }

        if ("CANCELLED".equalsIgnoreCase(appointment.status())) {
            log.warn("Appointment validation failed. reason=ALREADY_CANCELLED appointmentId={}", appointment.id());
            throw ApiException.badRequest("Appointment is already cancelled.");
        }
    }

    public void validateRescheduleAllowed(AppointmentRecord appointment) {
        if ("COMPLETED".equalsIgnoreCase(appointment.status())) {
            log.warn("Appointment validation failed. reason=COMPLETED_CANNOT_RESCHEDULE appointmentId={}", appointment.id());
            throw ApiException.badRequest("Completed appointment cannot be rescheduled.");
        }

        if ("CANCELLED".equalsIgnoreCase(appointment.status())) {
            log.warn("Appointment validation failed. reason=CANCELLED_CANNOT_RESCHEDULE appointmentId={}", appointment.id());
            throw ApiException.badRequest("Cancelled appointment cannot be rescheduled.");
        }
    }

    public void validateUpdateCount(int updated, Long appointmentId, Long tenantId, Long hospitalId) {
        if (updated == 0) {
            log.warn("Appointment validation failed. reason=APPOINTMENT_UPDATE_FAILED appointmentId={} tenantId={} hospitalId={}",
                    appointmentId, tenantId, hospitalId);
            throw ApiException.notFound("Appointment not found.");
        }
    }

    public void validateBookedSlots(List<SlotResponse> bookedSlots, Long doctorId, LocalDate date) {
        if (bookedSlots == null) {
            log.warn("Appointment validation failed. reason=BOOKED_SLOT_LIST_NULL doctorId={} date={}",
                    doctorId, date);
            throw ApiException.badRequest("Unable to fetch appointment availability.");
        }
    }
}