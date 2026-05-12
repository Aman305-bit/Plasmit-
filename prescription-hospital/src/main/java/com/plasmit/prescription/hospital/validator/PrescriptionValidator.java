package com.plasmit.prescription.hospital.validator;

import java.time.LocalDate;
import java.util.List;

import com.plasmit.prescription.hospital.exception.ApiException;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository.AdviceRecord;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository.DiagnosisRecord;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository.MedicineRecord;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository.PrescriptionRecord;
import com.plasmit.prescription.hospital.service.PrescriptionService.CreatePrescriptionRequest;
import com.plasmit.prescription.hospital.service.PrescriptionService.DiagnosisRequest;
import com.plasmit.prescription.hospital.service.PrescriptionService.MedicineRequest;
import com.plasmit.prescription.hospital.service.PrescriptionService.UpdatePrescriptionRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionValidator {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionValidator.class);

    public void validateTenantHospitalContext(Long tenantId, Long hospitalId) {

        if (tenantId == null || hospitalId == null) {
            log.warn(
                    "Prescription validation failed. reason=TENANT_OR_HOSPITAL_MISSING tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (tenantId <= 0 || hospitalId <= 0) {
            log.warn(
                    "Prescription validation failed. reason=INVALID_TENANT_OR_HOSPITAL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateBranchContext(Long branchId) {

        if (branchId != null && branchId <= 0) {
            log.warn("Prescription validation failed. reason=INVALID_BRANCH_ID branchId={}", branchId);
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
            log.warn("Prescription validation warning. reason=PAGE_SIZE_LIMIT_EXCEEDED requestedSize={}", size);
            return 100;
        }

        return size;
    }

    public void validateStatusFilter(String status) {

        if (status == null || status.isBlank()) {
            return;
        }

        if (!status.equalsIgnoreCase("ACTIVE")
                && !status.equalsIgnoreCase("CANCELLED")) {
            log.warn("Prescription validation failed. reason=INVALID_STATUS status={}", status);
            throw ApiException.badRequest("Status must be ACTIVE or CANCELLED.");
        }
    }

    public void validatePrescriptionId(Long prescriptionId) {

        if (prescriptionId == null || prescriptionId <= 0) {
            log.warn("Prescription validation failed. reason=INVALID_PRESCRIPTION_ID prescriptionId={}", prescriptionId);
            throw ApiException.badRequest("Invalid prescription id.");
        }
    }

    public void validatePatientId(Long patientId) {

        if (patientId == null || patientId <= 0) {
            log.warn("Prescription validation failed. reason=INVALID_PATIENT_ID patientId={}", patientId);
            throw ApiException.badRequest("Invalid patient id.");
        }
    }

    public void validateDoctorId(Long doctorId) {

        if (doctorId == null || doctorId <= 0) {
            log.warn("Prescription validation failed. reason=INVALID_DOCTOR_ID doctorId={}", doctorId);
            throw ApiException.badRequest("Invalid doctor id.");
        }
    }

    public void validateAppointmentId(Long appointmentId) {

        if (appointmentId != null && appointmentId <= 0) {
            log.warn("Prescription validation failed. reason=INVALID_APPOINTMENT_ID appointmentId={}", appointmentId);
            throw ApiException.badRequest("Invalid appointment id.");
        }
    }

    public void validateCreateRequest(CreatePrescriptionRequest request) {

        if (request == null) {
            log.warn("Prescription validation failed. reason=CREATE_REQUEST_NULL");
            throw ApiException.badRequest("Prescription request is required.");
        }

        validatePatientId(request.patientId());
        validateDoctorId(request.doctorId());
        validateAppointmentId(request.appointmentId());

        if (request.chiefComplaint() == null || request.chiefComplaint().isBlank()) {
            log.warn("Prescription validation failed. reason=CHIEF_COMPLAINT_REQUIRED");
            throw ApiException.badRequest("Chief complaint is required.");
        }

        validateFollowupDate(request.followupDate());
        validateMedicines(request.medicines());
        validateDiagnoses(request.diagnoses());
        validateAdviceList(request.adviceList());
    }

    public void validateUpdateRequest(UpdatePrescriptionRequest request) {

        if (request == null) {
            log.warn("Prescription validation failed. reason=UPDATE_REQUEST_NULL");
            throw ApiException.badRequest("Prescription request is required.");
        }

        if (request.chiefComplaint() == null || request.chiefComplaint().isBlank()) {
            log.warn("Prescription validation failed. reason=CHIEF_COMPLAINT_REQUIRED");
            throw ApiException.badRequest("Chief complaint is required.");
        }

        validateFollowupDate(request.followupDate());
        validateMedicines(request.medicines());
        validateDiagnoses(request.diagnoses());
        validateAdviceList(request.adviceList());
    }

    public void validateFollowupDate(LocalDate followupDate) {

        if (followupDate != null && followupDate.isBefore(LocalDate.now())) {
            log.warn("Prescription validation failed. reason=PAST_FOLLOWUP_DATE followupDate={}", followupDate);
            throw ApiException.badRequest("Follow-up date cannot be in the past.");
        }
    }

    public void validateMedicines(List<MedicineRequest> medicines) {

        if (medicines == null || medicines.isEmpty()) {
            log.warn("Prescription validation failed. reason=MEDICINE_LIST_REQUIRED");
            throw ApiException.badRequest("At least one medicine is required.");
        }

        for (MedicineRequest medicine : medicines) {
            validateMedicine(medicine);
        }
    }

    public void validateMedicine(MedicineRequest medicine) {

        if (medicine == null) {
            log.warn("Prescription validation failed. reason=MEDICINE_NULL");
            throw ApiException.badRequest("Medicine is required.");
        }

        if (medicine.medicineName() == null || medicine.medicineName().isBlank()) {
            log.warn("Prescription validation failed. reason=MEDICINE_NAME_REQUIRED");
            throw ApiException.badRequest("Medicine name is required.");
        }

        if (medicine.medicineName().length() > 200) {
            log.warn("Prescription validation failed. reason=MEDICINE_NAME_TOO_LONG medicineName={}", medicine.medicineName());
            throw ApiException.badRequest("Medicine name must be within 200 characters.");
        }
    }

    public void validateDiagnoses(List<DiagnosisRequest> diagnoses) {

        if (diagnoses == null) {
            return;
        }

        for (DiagnosisRequest diagnosis : diagnoses) {
            validateDiagnosis(diagnosis);
        }
    }

    public void validateDiagnosis(DiagnosisRequest diagnosis) {

        if (diagnosis == null) {
            log.warn("Prescription validation failed. reason=DIAGNOSIS_NULL");
            throw ApiException.badRequest("Diagnosis is required.");
        }

        if (diagnosis.diagnosisName() == null || diagnosis.diagnosisName().isBlank()) {
            log.warn("Prescription validation failed. reason=DIAGNOSIS_NAME_REQUIRED");
            throw ApiException.badRequest("Diagnosis name is required.");
        }
    }

    public void validateAdviceList(List<String> adviceList) {

        if (adviceList == null) {
            return;
        }

        for (String advice : adviceList) {
            if (advice != null && advice.length() > 500) {
                log.warn("Prescription validation failed. reason=ADVICE_TOO_LONG advice={}", advice);
                throw ApiException.badRequest("Advice must be within 500 characters.");
            }
        }
    }

    public void validatePatientExists(boolean exists, Long patientId, Long tenantId, Long hospitalId) {

        if (!exists) {
            log.warn(
                    "Prescription validation failed. reason=PATIENT_NOT_FOUND patientId={} tenantId={} hospitalId={}",
                    patientId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Patient not found.");
        }
    }

    public void validateDoctorExists(boolean exists, Long doctorId, Long tenantId, Long hospitalId) {

        if (!exists) {
            log.warn(
                    "Prescription validation failed. reason=DOCTOR_NOT_FOUND doctorId={} tenantId={} hospitalId={}",
                    doctorId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Doctor not found.");
        }
    }

    public void validateAppointmentExists(
            boolean exists,
            Long appointmentId,
            Long patientId,
            Long doctorId
    ) {

        if (!exists) {
            log.warn(
                    "Prescription validation failed. reason=APPOINTMENT_NOT_FOUND appointmentId={} patientId={} doctorId={}",
                    appointmentId,
                    patientId,
                    doctorId
            );
            throw ApiException.notFound("Appointment not found for selected patient and doctor.");
        }
    }

    public void validatePrescriptionFound(
            PrescriptionRecord prescription,
            Long prescriptionId,
            Long tenantId,
            Long hospitalId
    ) {

        if (prescription == null) {
            log.warn(
                    "Prescription validation failed. reason=PRESCRIPTION_NOT_FOUND prescriptionId={} tenantId={} hospitalId={}",
                    prescriptionId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Prescription not found.");
        }
    }

    public void validatePrescriptionActive(PrescriptionRecord prescription) {

        if (!"ACTIVE".equalsIgnoreCase(prescription.status())) {
            log.warn(
                    "Prescription validation failed. reason=PRESCRIPTION_NOT_ACTIVE prescriptionId={} status={}",
                    prescription.id(),
                    prescription.status()
            );
            throw ApiException.badRequest("Only ACTIVE prescription can be updated.");
        }
    }

    public void validatePrescriptionList(
            List<PrescriptionRecord> prescriptions,
            Long tenantId,
            Long hospitalId
    ) {

        if (prescriptions == null) {
            log.warn(
                    "Prescription validation failed. reason=PRESCRIPTION_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch prescriptions.");
        }
    }

    public void validateDetailLists(
            List<MedicineRecord> medicines,
            List<DiagnosisRecord> diagnoses,
            List<AdviceRecord> adviceList,
            Long prescriptionId
    ) {

        if (medicines == null) {
            log.warn("Prescription validation failed. reason=MEDICINE_RECORD_LIST_NULL prescriptionId={}", prescriptionId);
            throw ApiException.badRequest("Unable to fetch prescription medicines.");
        }

        if (diagnoses == null) {
            log.warn("Prescription validation failed. reason=DIAGNOSIS_RECORD_LIST_NULL prescriptionId={}", prescriptionId);
            throw ApiException.badRequest("Unable to fetch prescription diagnoses.");
        }

        if (adviceList == null) {
            log.warn("Prescription validation failed. reason=ADVICE_RECORD_LIST_NULL prescriptionId={}", prescriptionId);
            throw ApiException.badRequest("Unable to fetch prescription advice.");
        }
    }

    public void validateUpdateCount(
            int updated,
            Long prescriptionId,
            Long tenantId,
            Long hospitalId
    ) {

        if (updated == 0) {
            log.warn(
                    "Prescription validation failed. reason=PRESCRIPTION_UPDATE_FAILED prescriptionId={} tenantId={} hospitalId={}",
                    prescriptionId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Prescription not found.");
        }
    }

    public void validateCancelCount(
            int updated,
            Long prescriptionId,
            Long tenantId,
            Long hospitalId
    ) {

        if (updated == 0) {
            log.warn(
                    "Prescription validation failed. reason=PRESCRIPTION_CANCEL_FAILED prescriptionId={} tenantId={} hospitalId={}",
                    prescriptionId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Prescription not found or already cancelled.");
        }
    }
}