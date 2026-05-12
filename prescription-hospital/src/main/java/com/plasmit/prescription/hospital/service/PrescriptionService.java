package com.plasmit.prescription.hospital.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.plasmit.prescription.hospital.exception.ApiException;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository.AdviceRecord;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository.DiagnosisRecord;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository.MedicineRecord;
import com.plasmit.prescription.hospital.repository.PrescriptionRepository.PrescriptionRecord;
import com.plasmit.prescription.hospital.security.SecurityConfig.CurrentUser;
import com.plasmit.prescription.hospital.security.SecurityConfig.TenantContext;
import com.plasmit.prescription.hospital.validator.PrescriptionValidator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);

    private final PrescriptionRepository repository;
    private final PrescriptionValidator validator;

    public PrescriptionService(
            PrescriptionRepository repository,
            PrescriptionValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<PrescriptionRecord> listPrescriptions(
            Long patientId,
            Long doctorId,
            Long appointmentId,
            String status,
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

        if (patientId != null) {
            validator.validatePatientId(patientId);
        }

        if (doctorId != null) {
            validator.validateDoctorId(doctorId);
        }

        if (appointmentId != null) {
            validator.validateAppointmentId(appointmentId);
        }

        requirePermission(currentUser, "prescriptions.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        log.info("List prescriptions request. tenantId={} hospitalId={} branchId={} patientId={} doctorId={} appointmentId={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                patientId,
                doctorId,
                appointmentId);

        List<PrescriptionRecord> prescriptions = repository.findPrescriptions(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                patientId,
                doctorId,
                appointmentId,
                status,
                safeSize,
                offset
        );

        validator.validatePrescriptionList(
                prescriptions,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return prescriptions;
    }

    public PrescriptionDetailResponse getPrescription(Long prescriptionId) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePrescriptionId(prescriptionId);

        requirePermission(currentUser, "prescriptions.view");

        PrescriptionRecord prescription = repository.findById(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validatePrescriptionFound(
                prescription,
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return buildDetail(currentUser, prescription);
    }

    @Transactional
    public PrescriptionDetailResponse createPrescription(CreatePrescriptionRequest request) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateRequest(request);

        requirePermission(currentUser, "prescriptions.create");

        log.info("Create prescription request. tenantId={} hospitalId={} patientId={} doctorId={} appointmentId={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.patientId(),
                request.doctorId(),
                request.appointmentId());

        validateRelations(
                currentUser,
                request.patientId(),
                request.doctorId(),
                request.appointmentId()
        );

        String prescriptionNumber = repository.generateNextPrescriptionNumber(currentUser.getHospitalId());

        Long prescriptionId = repository.createPrescription(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getDepartmentId(),
                currentUser.getUserId(),
                prescriptionNumber,
                request
        );

        repository.replaceMedicines(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.medicines()
        );

        repository.replaceDiagnoses(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.diagnoses()
        );

        repository.replaceAdvice(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.adviceList()
        );

        log.info("Prescription created successfully. prescriptionId={} prescriptionNumber={}",
                prescriptionId,
                prescriptionNumber);

        return getPrescription(prescriptionId);
    }

    @Transactional
    public PrescriptionDetailResponse updatePrescription(Long prescriptionId, UpdatePrescriptionRequest request) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePrescriptionId(prescriptionId);
        validator.validateUpdateRequest(request);

        requirePermission(currentUser, "prescriptions.update");

        PrescriptionRecord existing = repository.findById(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validatePrescriptionFound(
                existing,
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validatePrescriptionActive(existing);

        int updated = repository.updatePrescription(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request
        );

        validator.validateUpdateCount(
                updated,
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        repository.replaceMedicines(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.medicines()
        );

        repository.replaceDiagnoses(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.diagnoses()
        );

        repository.replaceAdvice(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.adviceList()
        );

        log.info("Prescription updated successfully. prescriptionId={} userId={}",
                prescriptionId,
                currentUser.getUserId());

        return getPrescription(prescriptionId);
    }

    @Transactional
    public Map<String, Object> cancelPrescription(Long prescriptionId) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePrescriptionId(prescriptionId);

        requirePermission(currentUser, "prescriptions.cancel");

        int updated = repository.cancelPrescription(
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId()
        );

        validator.validateCancelCount(
                updated,
                prescriptionId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Prescription cancelled successfully. prescriptionId={} userId={}",
                prescriptionId,
                currentUser.getUserId());

        return Map.of(
                "prescriptionId", prescriptionId,
                "cancelled", true
        );
    }

    public List<PrescriptionRecord> patientHistory(Long patientId) {
        validator.validatePatientId(patientId);
        return listPrescriptions(patientId, null, null, "ACTIVE", 1, 100);
    }

    public List<PrescriptionRecord> appointmentPrescriptions(Long appointmentId) {
        validator.validateAppointmentId(appointmentId);
        return listPrescriptions(null, null, appointmentId, null, 1, 20);
    }

    private PrescriptionDetailResponse buildDetail(
            CurrentUser currentUser,
            PrescriptionRecord prescription
    ) {
        List<MedicineRecord> medicines = repository.findMedicines(
                prescription.id(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<DiagnosisRecord> diagnoses = repository.findDiagnoses(
                prescription.id(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<AdviceRecord> adviceList = repository.findAdvice(
                prescription.id(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateDetailLists(
                medicines,
                diagnoses,
                adviceList,
                prescription.id()
        );

        return new PrescriptionDetailResponse(
                prescription,
                medicines,
                diagnoses,
                adviceList
        );
    }

    private void validateRelations(
            CurrentUser currentUser,
            Long patientId,
            Long doctorId,
            Long appointmentId
    ) {
        validator.validatePatientId(patientId);
        validator.validateDoctorId(doctorId);
        validator.validateAppointmentId(appointmentId);

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

        boolean appointmentExists = repository.appointmentExists(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                patientId,
                doctorId,
                appointmentId
        );

        validator.validateAppointmentExists(
                appointmentExists,
                appointmentId,
                patientId,
                doctorId
        );

        log.info("Prescription relations validated. patientId={} doctorId={} appointmentId={}",
                patientId,
                doctorId,
                appointmentId);
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

    public record CreatePrescriptionRequest(
            @NotNull(message = "Patient id is required.")
            Long patientId,

            @NotNull(message = "Doctor id is required.")
            Long doctorId,

            Long appointmentId,

            @NotBlank(message = "Chief complaint is required.")
            String chiefComplaint,

            String clinicalNotes,
            String diagnosisSummary,
            String adviceSummary,
            LocalDate followupDate,

            List<@Valid MedicineRequest> medicines,
            List<@Valid DiagnosisRequest> diagnoses,
            List<String> adviceList
    ) {
    }

    public record UpdatePrescriptionRequest(
            @NotBlank(message = "Chief complaint is required.")
            String chiefComplaint,

            String clinicalNotes,
            String diagnosisSummary,
            String adviceSummary,
            LocalDate followupDate,

            List<@Valid MedicineRequest> medicines,
            List<@Valid DiagnosisRequest> diagnoses,
            List<String> adviceList
    ) {
    }

    public record MedicineRequest(
            @NotBlank(message = "Medicine name is required.")
            @Size(max = 200, message = "Medicine name must be within 200 characters.")
            String medicineName,

            String dosage,
            String frequency,
            String duration,
            String route,
            String timing,
            String instructions
    ) {
    }

    public record DiagnosisRequest(
            @NotBlank(message = "Diagnosis name is required.")
            String diagnosisName,

            String diagnosisCode,
            String notes
    ) {
    }

    public record PrescriptionDetailResponse(
            PrescriptionRecord prescription,
            List<MedicineRecord> medicines,
            List<DiagnosisRecord> diagnoses,
            List<AdviceRecord> adviceList
    ) {
    }
}