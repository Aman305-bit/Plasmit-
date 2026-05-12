package com.plasmit.lab.hospital.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.plasmit.lab.hospital.exception.ApiException;
import com.plasmit.lab.hospital.repository.LabRepository;
import com.plasmit.lab.hospital.repository.LabRepository.BillingServiceRecord;
import com.plasmit.lab.hospital.repository.LabRepository.PackageItemRecord;
import com.plasmit.lab.hospital.repository.LabRepository.PackageRecord;
import com.plasmit.lab.hospital.repository.LabRepository.TestRecord;
import com.plasmit.lab.hospital.security.SecurityConfig.CurrentUser;
import com.plasmit.lab.hospital.security.SecurityConfig.TenantContext;
import com.plasmit.lab.hospital.validator.LabValidator;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabService {

    private static final Logger log = LoggerFactory.getLogger(LabService.class);

    private final LabRepository repository;
    private final LabValidator validator;

    public LabService(
            LabRepository repository,
            LabValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<TestRecord> listTests(
            String query,
            String category,
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

        requirePermission(currentUser, "test_catalog.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        log.info("List lab tests request. userId={} tenantId={} hospitalId={} branchId={} page={} size={}",
                currentUser.getUserId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                safePage,
                safeSize);

        List<TestRecord> tests = repository.findTests(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query,
                category,
                status,
                safeSize,
                offset
        );

        validator.validateTestList(
                tests,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return tests;
    }

    public TestRecord getTest(Long testId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateTestId(testId);

        requirePermission(currentUser, "test_catalog.view");

        log.info("Lab test detail request. testId={} tenantId={} hospitalId={}",
                testId,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        TestRecord test = repository.findTestById(
                testId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateTestFound(
                test,
                testId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return test;
    }

    @Transactional
    public TestRecord createTest(CreateTestRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateTestRequest(request);

        requirePermission(currentUser, "test_catalog.create");

        String testCode = repository.generateNextTestCode(currentUser.getHospitalId());

        log.info("Create lab test request. tenantId={} hospitalId={} testName={} testCode={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.testName(),
                testCode);

        Long testId = repository.createTest(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                testCode,
                request
        );

        log.info("Lab test created successfully. testId={} testCode={}", testId, testCode);

        return getTest(testId);
    }

    @Transactional
    public TestRecord updateTest(Long testId, UpdateTestRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateTestId(testId);
        validator.validateUpdateTestRequest(request);

        requirePermission(currentUser, "test_catalog.update");

        TestRecord existing = repository.findTestById(
                testId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateTestFound(
                existing,
                testId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Update lab test request. testId={} tenantId={} hospitalId={}",
                testId,
                currentUser.getTenantId(),
                currentUser.getHospitalId());

        int updated = repository.updateTest(
                testId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request
        );

        validator.validateUpdateCount(
                updated,
                testId,
                "TEST",
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return getTest(testId);
    }

    @Transactional
    public Map<String, Object> archiveTest(Long testId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateTestId(testId);

        requirePermission(currentUser, "test_catalog.archive");

        int updated = repository.archiveTest(
                testId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId()
        );

        validator.validateArchiveCount(
                updated,
                testId,
                "TEST",
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Lab test archived successfully. testId={} userId={}", testId, currentUser.getUserId());

        return Map.of(
                "testId", testId,
                "archived", true
        );
    }

    public List<PackageRecord> listPackages(
            String query,
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

        requirePermission(currentUser, "test_packages.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        log.info("List lab packages request. userId={} tenantId={} hospitalId={} branchId={} page={} size={}",
                currentUser.getUserId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                safePage,
                safeSize);

        List<PackageRecord> packages = repository.findPackages(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query,
                status,
                safeSize,
                offset
        );

        validator.validatePackageList(
                packages,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return packages;
    }

    public PackageDetailResponse getPackage(Long packageId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePackageId(packageId);

        requirePermission(currentUser, "test_packages.view");

        PackageRecord packageRecord = repository.findPackageById(
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validatePackageFound(
                packageRecord,
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<PackageItemRecord> items = repository.findPackageItems(
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validatePackageItems(
                items,
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return new PackageDetailResponse(packageRecord, items);
    }

    @Transactional
    public PackageDetailResponse createPackage(CreatePackageRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreatePackageRequest(request);

        requirePermission(currentUser, "test_packages.create");

        boolean allTestsExist = repository.allTestsExist(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.testIds()
        );

        validator.validateAllTestsExist(
                allTestsExist,
                request.testIds(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        String packageCode = repository.generateNextPackageCode(currentUser.getHospitalId());

        log.info("Create lab package request. tenantId={} hospitalId={} packageName={} itemCount={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.packageName(),
                request.testIds() == null ? 0 : request.testIds().size());

        Long packageId = repository.createPackage(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                packageCode,
                request
        );

        repository.replacePackageItems(
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request.testIds()
        );

        log.info("Lab package created successfully. packageId={} packageCode={}", packageId, packageCode);

        return getPackage(packageId);
    }

    @Transactional
    public PackageDetailResponse updatePackage(Long packageId, UpdatePackageRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePackageId(packageId);
        validator.validateUpdatePackageRequest(request);

        requirePermission(currentUser, "test_packages.update");

        PackageRecord existing = repository.findPackageById(
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validatePackageFound(
                existing,
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        boolean allTestsExist = repository.allTestsExist(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.testIds()
        );

        validator.validateAllTestsExist(
                allTestsExist,
                request.testIds(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        int updated = repository.updatePackage(
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request
        );

        validator.validateUpdateCount(
                updated,
                packageId,
                "PACKAGE",
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        repository.replacePackageItems(
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                request.testIds()
        );

        log.info("Lab package updated successfully. packageId={}", packageId);

        return getPackage(packageId);
    }

    @Transactional
    public Map<String, Object> archivePackage(Long packageId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePackageId(packageId);

        requirePermission(currentUser, "test_packages.archive");

        int updated = repository.archivePackage(
                packageId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId()
        );

        validator.validateArchiveCount(
                updated,
                packageId,
                "PACKAGE",
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        log.info("Lab package archived successfully. packageId={} userId={}", packageId, currentUser.getUserId());

        return Map.of(
                "packageId", packageId,
                "archived", true
        );
    }

    public List<BillingServiceRecord> billingServices(String query) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());

        requirePermission(currentUser, "billing.view");

        log.info("Billing services request. tenantId={} hospitalId={} branchId={} query={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query);

        List<BillingServiceRecord> services = repository.findBillingServices(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query
        );

        validator.validateBillingServices(
                services,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return services;
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

    public record CreateTestRequest(
            @NotBlank(message = "Test name is required.")
            @Size(max = 200, message = "Test name must be within 200 characters.")
            String testName,

            String testCategory,
            String sampleType,
            String reportType,
            String unitName,
            String normalRange,
            String interpretation,

            @NotNull(message = "Price is required.")
            @DecimalMin(value = "0.0", message = "Price cannot be negative.")
            BigDecimal price,

            @DecimalMin(value = "0.0", message = "Tax rate cannot be negative.")
            BigDecimal taxRate,

            Integer turnaroundTimeHours
    ) {
    }

    public record UpdateTestRequest(
            @NotBlank(message = "Test name is required.")
            @Size(max = 200, message = "Test name must be within 200 characters.")
            String testName,

            String testCategory,
            String sampleType,
            String reportType,
            String unitName,
            String normalRange,
            String interpretation,

            @NotNull(message = "Price is required.")
            @DecimalMin(value = "0.0", message = "Price cannot be negative.")
            BigDecimal price,

            @DecimalMin(value = "0.0", message = "Tax rate cannot be negative.")
            BigDecimal taxRate,

            Integer turnaroundTimeHours
    ) {
    }

    public record CreatePackageRequest(
            @NotBlank(message = "Package name is required.")
            String packageName,

            String packageCategory,
            String description,

            @NotNull(message = "Package price is required.")
            @DecimalMin(value = "0.0", message = "Package price cannot be negative.")
            BigDecimal packagePrice,

            @DecimalMin(value = "0.0", message = "Tax rate cannot be negative.")
            BigDecimal taxRate,

            List<Long> testIds
    ) {
    }

    public record UpdatePackageRequest(
            @NotBlank(message = "Package name is required.")
            String packageName,

            String packageCategory,
            String description,

            @NotNull(message = "Package price is required.")
            @DecimalMin(value = "0.0", message = "Package price cannot be negative.")
            BigDecimal packagePrice,

            @DecimalMin(value = "0.0", message = "Tax rate cannot be negative.")
            BigDecimal taxRate,

            List<Long> testIds
    ) {
    }

    public record PackageDetailResponse(
            PackageRecord packageInfo,
            List<PackageItemRecord> items
    ) {
    }
}