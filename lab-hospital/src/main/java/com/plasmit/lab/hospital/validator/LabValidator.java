package com.plasmit.lab.hospital.validator;

import java.math.BigDecimal;
import java.util.List;

import com.plasmit.lab.hospital.exception.ApiException;
import com.plasmit.lab.hospital.repository.LabRepository.BillingServiceRecord;
import com.plasmit.lab.hospital.repository.LabRepository.PackageItemRecord;
import com.plasmit.lab.hospital.repository.LabRepository.PackageRecord;
import com.plasmit.lab.hospital.repository.LabRepository.TestRecord;
import com.plasmit.lab.hospital.service.LabService.CreatePackageRequest;
import com.plasmit.lab.hospital.service.LabService.CreateTestRequest;
import com.plasmit.lab.hospital.service.LabService.UpdatePackageRequest;
import com.plasmit.lab.hospital.service.LabService.UpdateTestRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LabValidator {

    private static final Logger log = LoggerFactory.getLogger(LabValidator.class);

    public void validateTenantHospitalContext(Long tenantId, Long hospitalId) {

        if (tenantId == null || hospitalId == null) {
            log.warn("Lab validation failed. reason=TENANT_OR_HOSPITAL_MISSING tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (tenantId <= 0 || hospitalId <= 0) {
            log.warn("Lab validation failed. reason=INVALID_TENANT_OR_HOSPITAL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateBranchContext(Long branchId) {

        if (branchId != null && branchId <= 0) {
            log.warn("Lab validation failed. reason=INVALID_BRANCH_ID branchId={}", branchId);
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
            log.warn("Lab validation warning. reason=PAGE_SIZE_LIMIT_EXCEEDED requestedSize={}", size);
            return 100;
        }

        return size;
    }

    public void validateStatusFilter(String status) {

        if (status == null || status.isBlank()) {
            return;
        }

        if (!status.equalsIgnoreCase("ACTIVE")
                && !status.equalsIgnoreCase("INACTIVE")
                && !status.equalsIgnoreCase("ARCHIVED")) {
            log.warn("Lab validation failed. reason=INVALID_STATUS status={}", status);
            throw ApiException.badRequest("Status must be ACTIVE, INACTIVE or ARCHIVED.");
        }
    }

    public void validateTestId(Long testId) {

        if (testId == null || testId <= 0) {
            log.warn("Lab validation failed. reason=INVALID_TEST_ID testId={}", testId);
            throw ApiException.badRequest("Invalid test id.");
        }
    }

    public void validatePackageId(Long packageId) {

        if (packageId == null || packageId <= 0) {
            log.warn("Lab validation failed. reason=INVALID_PACKAGE_ID packageId={}", packageId);
            throw ApiException.badRequest("Invalid package id.");
        }
    }

    public void validateTestFound(TestRecord test, Long testId, Long tenantId, Long hospitalId) {

        if (test == null) {
            log.warn("Lab validation failed. reason=TEST_NOT_FOUND testId={} tenantId={} hospitalId={}",
                    testId, tenantId, hospitalId);
            throw ApiException.notFound("Lab test not found.");
        }
    }

    public void validatePackageFound(PackageRecord packageRecord, Long packageId, Long tenantId, Long hospitalId) {

        if (packageRecord == null) {
            log.warn("Lab validation failed. reason=PACKAGE_NOT_FOUND packageId={} tenantId={} hospitalId={}",
                    packageId, tenantId, hospitalId);
            throw ApiException.notFound("Lab package not found.");
        }
    }

    public void validateTestList(List<TestRecord> tests, Long tenantId, Long hospitalId) {

        if (tests == null) {
            log.warn("Lab validation failed. reason=TEST_LIST_NULL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.badRequest("Unable to fetch lab tests.");
        }
    }

    public void validatePackageList(List<PackageRecord> packages, Long tenantId, Long hospitalId) {

        if (packages == null) {
            log.warn("Lab validation failed. reason=PACKAGE_LIST_NULL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.badRequest("Unable to fetch lab packages.");
        }
    }

    public void validatePackageItems(List<PackageItemRecord> items, Long packageId, Long tenantId, Long hospitalId) {

        if (items == null) {
            log.warn("Lab validation failed. reason=PACKAGE_ITEM_LIST_NULL packageId={} tenantId={} hospitalId={}",
                    packageId, tenantId, hospitalId);
            throw ApiException.badRequest("Unable to fetch lab package items.");
        }
    }

    public void validateBillingServices(List<BillingServiceRecord> services, Long tenantId, Long hospitalId) {

        if (services == null) {
            log.warn("Lab validation failed. reason=BILLING_SERVICE_LIST_NULL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.badRequest("Unable to fetch billing services.");
        }
    }

    public void validateCreateTestRequest(CreateTestRequest request) {

        if (request == null) {
            log.warn("Lab validation failed. reason=CREATE_TEST_REQUEST_NULL");
            throw ApiException.badRequest("Lab test request is required.");
        }

        validateAmount(request.price(), "Price");
        validateAmount(request.taxRate(), "Tax rate");
        validateTurnaroundTime(request.turnaroundTimeHours());
    }

    public void validateUpdateTestRequest(UpdateTestRequest request) {

        if (request == null) {
            log.warn("Lab validation failed. reason=UPDATE_TEST_REQUEST_NULL");
            throw ApiException.badRequest("Lab test request is required.");
        }

        validateAmount(request.price(), "Price");
        validateAmount(request.taxRate(), "Tax rate");
        validateTurnaroundTime(request.turnaroundTimeHours());
    }

    public void validateCreatePackageRequest(CreatePackageRequest request) {

        if (request == null) {
            log.warn("Lab validation failed. reason=CREATE_PACKAGE_REQUEST_NULL");
            throw ApiException.badRequest("Lab package request is required.");
        }

        validateAmount(request.packagePrice(), "Package price");
        validateAmount(request.taxRate(), "Tax rate");
        validatePackageTestIds(request.testIds());
    }

    public void validateUpdatePackageRequest(UpdatePackageRequest request) {

        if (request == null) {
            log.warn("Lab validation failed. reason=UPDATE_PACKAGE_REQUEST_NULL");
            throw ApiException.badRequest("Lab package request is required.");
        }

        validateAmount(request.packagePrice(), "Package price");
        validateAmount(request.taxRate(), "Tax rate");
        validatePackageTestIds(request.testIds());
    }

    public void validatePackageTestIds(List<Long> testIds) {

        if (testIds == null || testIds.isEmpty()) {
            log.warn("Lab validation failed. reason=PACKAGE_TEST_IDS_REQUIRED");
            throw ApiException.badRequest("At least one lab test is required in package.");
        }

        for (Long testId : testIds) {
            validateTestId(testId);
        }
    }

    public void validateAllTestsExist(boolean exists, List<Long> testIds, Long tenantId, Long hospitalId) {

        if (!exists) {
            log.warn("Lab validation failed. reason=INVALID_PACKAGE_TEST_IDS tenantId={} hospitalId={} testIds={}",
                    tenantId, hospitalId, testIds);
            throw ApiException.badRequest("One or more selected lab tests are invalid.");
        }
    }

    public void validateUpdateCount(int updated, Long id, String entity, Long tenantId, Long hospitalId) {

        if (updated == 0) {
            log.warn("Lab validation failed. reason={}_UPDATE_FAILED id={} tenantId={} hospitalId={}",
                    entity, id, tenantId, hospitalId);

            if ("TEST".equals(entity)) {
                throw ApiException.notFound("Lab test not found.");
            }

            throw ApiException.notFound("Lab package not found.");
        }
    }

    public void validateArchiveCount(int updated, Long id, String entity, Long tenantId, Long hospitalId) {

        if (updated == 0) {
            log.warn("Lab validation failed. reason={}_ARCHIVE_FAILED id={} tenantId={} hospitalId={}",
                    entity, id, tenantId, hospitalId);

            if ("TEST".equals(entity)) {
                throw ApiException.notFound("Lab test not found.");
            }

            throw ApiException.notFound("Lab package not found.");
        }
    }

    private void validateAmount(BigDecimal amount, String fieldName) {

        if (amount == null) {
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Lab validation failed. reason=NEGATIVE_AMOUNT field={} amount={}", fieldName, amount);
            throw ApiException.badRequest(fieldName + " cannot be negative.");
        }
    }

    private void validateTurnaroundTime(Integer hours) {

        if (hours == null) {
            return;
        }

        if (hours < 0 || hours > 720) {
            log.warn("Lab validation failed. reason=INVALID_TURNAROUND_TIME hours={}", hours);
            throw ApiException.badRequest("Turnaround time must be between 0 and 720 hours.");
        }
    }
}