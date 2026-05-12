package com.plasmit.hospital.core.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.plasmit.hospital.core.exception.ApiException;
import com.plasmit.hospital.core.repository.HospitalCoreRepository;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.BranchOption;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.DepartmentOption;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.HospitalProfile;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.ReferenceOption;
import com.plasmit.hospital.core.repository.HospitalCoreRepository.SettingRecord;
import com.plasmit.hospital.core.security.SecurityConfig.CurrentUser;
import com.plasmit.hospital.core.security.SecurityConfig.TenantContext;
import com.plasmit.hospital.core.validator.HospitalCoreValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HospitalCoreService {

    private static final Logger log = LoggerFactory.getLogger(HospitalCoreService.class);

    private final HospitalCoreRepository repository;
    private final HospitalCoreValidator validator;

    public HospitalCoreService(
            HospitalCoreRepository repository,
            HospitalCoreValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    public SettingsResponse getSettings() {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());

        requirePermission(currentUser, "settings.view");

        log.info("Fetching hospital settings. userId={} tenantId={} hospitalId={}",
                currentUser.getUserId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        HospitalProfile hospital = repository.findHospitalProfile(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        ).orElse(null);

        validator.validateHospitalProfile(
                hospital,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<BranchOption> branches = repository.findBranches(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranches(
                branches,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<DepartmentOption> departments = repository.findDepartments(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        );

        validator.validateDepartments(
                departments,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<SettingRecord> settingRecords = repository.findSettings(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateSettings(
                settingRecords,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        Map<String, String> settings = settingRecords.stream()
                .collect(Collectors.toMap(
                        SettingRecord::key,
                        SettingRecord::value,
                        (first, second) -> second
                ));

        return new SettingsResponse(
                hospital,
                branches,
                departments,
                settings
        );
    }

    public List<BranchOption> getBranches() {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        requirePermission(currentUser, "settings.view");

        log.info("Fetching branch reference. tenantId={} hospitalId={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        List<BranchOption> branches = repository.findBranches(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranches(
                branches,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return branches;
    }

    public List<DepartmentOption> getDepartments() {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());

        requirePermission(currentUser, "settings.view");

        log.info("Fetching department reference. tenantId={} hospitalId={} branchId={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId());

        List<DepartmentOption> departments = repository.findDepartments(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        );

        validator.validateDepartments(
                departments,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return departments;
    }

    public List<ReferenceOption> getBloodGroups() {
        return getReference("BLOOD_GROUP", "patients.view");
    }

    public List<ReferenceOption> getPaymentMethods() {
        return getReference("PAYMENT_METHOD", "billing.view");
    }

    public List<ReferenceOption> getAppointmentTypes() {
        return getReference("APPOINTMENT_TYPE", "appointments.view");
    }

    public List<ReferenceOption> getSourceChannels() {
        return getReference("SOURCE_CHANNEL", "billing.view");
    }

    public List<ReferenceOption> getPayerTypes() {
        return getReference("PAYER_TYPE", "patients.view");
    }

    private List<ReferenceOption> getReference(String referenceType, String permission) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateReferenceType(referenceType);

        requirePermission(currentUser, permission);

        log.info("Fetching reference. referenceType={} tenantId={} hospitalId={}",
                referenceType,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        List<ReferenceOption> references = repository.findReferenceValues(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                referenceType
        );

        validator.validateReferenceValues(
                referenceType,
                references,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return references;
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

    public record SettingsResponse(
            HospitalProfile hospital,
            List<BranchOption> branches,
            List<DepartmentOption> departments,
            Map<String, String> settings
    ) {
    }
}