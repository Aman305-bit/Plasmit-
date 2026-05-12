package com.plasmit.subscription.validator;

import com.plasmit.subscription.dto.request.CreateHospitalSubscriptionRequest;
import com.plasmit.subscription.dto.request.CreatePlanRequest;
import com.plasmit.subscription.dto.request.UpdateHospitalSubscriptionRequest;
import com.plasmit.subscription.dto.request.UpdatePlanRequest;
import com.plasmit.subscription.dto.request.UpdatePlanStatusRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SubscriptionValidator {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionValidator.class);

    public void validatePlanId(Long planId) {
        if (planId == null || planId <= 0) {
            log.warn("Subscription validation failed. reason=INVALID_PLAN_ID planId={}", planId);
            throw new IllegalArgumentException("Invalid subscription plan id");
        }
    }

    public void validateMappingId(Long mappingId) {
        if (mappingId == null || mappingId <= 0) {
            log.warn("Subscription validation failed. reason=INVALID_MAPPING_ID mappingId={}", mappingId);
            throw new IllegalArgumentException("Invalid hospital subscription mapping id");
        }
    }

    public void validateHospitalId(Long hospitalId) {
        if (hospitalId == null || hospitalId <= 0) {
            log.warn("Subscription validation failed. reason=INVALID_HOSPITAL_ID hospitalId={}", hospitalId);
            throw new IllegalArgumentException("Invalid hospital id");
        }
    }

    public void validateOptionalHospitalId(Long hospitalId) {
        if (hospitalId != null && hospitalId <= 0) {
            log.warn("Subscription validation failed. reason=INVALID_HOSPITAL_ID hospitalId={}", hospitalId);
            throw new IllegalArgumentException("Invalid hospital id");
        }
    }

    public void validateOptionalPlanId(Long planId) {
        if (planId != null && planId <= 0) {
            log.warn("Subscription validation failed. reason=INVALID_PLAN_ID planId={}", planId);
            throw new IllegalArgumentException("Invalid subscription plan id");
        }
    }

    public void validateCreatePlan(CreatePlanRequest request) {
        if (request == null) {
            log.warn("Subscription validation failed. reason=CREATE_PLAN_REQUEST_NULL");
            throw new IllegalArgumentException("Subscription plan request is required");
        }

        validatePlanName(request.getName());
        validateBillingCycle(request.getBillingCycle());
        validatePrice(request.getPrice());
        validateCurrency(request.getCurrency());
        validateLimits(
                request.getUserLimit(),
                request.getBranchLimit(),
                request.getStorageLimitGb()
        );
    }

    public void validateUpdatePlan(Long planId, UpdatePlanRequest request) {
        validatePlanId(planId);

        if (request == null) {
            log.warn("Subscription validation failed. reason=UPDATE_PLAN_REQUEST_NULL planId={}", planId);
            throw new IllegalArgumentException("Subscription plan request is required");
        }

        validatePlanName(request.getName());
        validateBillingCycle(request.getBillingCycle());
        validatePrice(request.getPrice());
        validateCurrency(request.getCurrency());
        validateLimits(
                request.getUserLimit(),
                request.getBranchLimit(),
                request.getStorageLimitGb()
        );
    }

    public void validateUpdatePlanStatus(Long planId, UpdatePlanStatusRequest request) {
        validatePlanId(planId);

        if (request == null) {
            log.warn("Subscription validation failed. reason=UPDATE_PLAN_STATUS_REQUEST_NULL planId={}", planId);
            throw new IllegalArgumentException("Plan status request is required");
        }

        validatePlanStatus(request.getStatus());
    }

    public void validateCreateHospitalMapping(CreateHospitalSubscriptionRequest request) {
        if (request == null) {
            log.warn("Subscription validation failed. reason=CREATE_MAPPING_REQUEST_NULL");
            throw new IllegalArgumentException("Hospital subscription request is required");
        }

        validateHospitalId(request.getHospitalId());
        validatePlanId(request.getPlanId());
        validateSubscriptionStatusWithDefault(request.getStatus());
        validatePaymentStatusWithDefault(request.getPaymentStatus());

        if (request.getStartDate() == null) {
            log.warn("Subscription validation failed. reason=START_DATE_REQUIRED hospitalId={} planId={}",
                    request.getHospitalId(), request.getPlanId());
            throw new IllegalArgumentException("Start date is required");
        }

        if (request.getEndDate() == null) {
            log.warn("Subscription validation failed. reason=END_DATE_REQUIRED hospitalId={} planId={}",
                    request.getHospitalId(), request.getPlanId());
            throw new IllegalArgumentException("End date is required");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            log.warn("Subscription validation failed. reason=END_DATE_BEFORE_START_DATE hospitalId={} planId={} startDate={} endDate={}",
                    request.getHospitalId(),
                    request.getPlanId(),
                    request.getStartDate(),
                    request.getEndDate());
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    public void validateUpdateHospitalMapping(Long mappingId, UpdateHospitalSubscriptionRequest request) {
        validateMappingId(mappingId);

        if (request == null) {
            log.warn("Subscription validation failed. reason=UPDATE_MAPPING_REQUEST_NULL mappingId={}", mappingId);
            throw new IllegalArgumentException("Hospital subscription request is required");
        }

        validatePlanId(request.getPlanId());
        validateSubscriptionStatusWithDefault(request.getStatus());
        validatePaymentStatusWithDefault(request.getPaymentStatus());
    }

    public void validateGetHospitalMappings(Long hospitalId, Long planId, String status) {
        validateOptionalHospitalId(hospitalId);
        validateOptionalPlanId(planId);

        if (status == null || status.isBlank()) {
            return;
        }

        validateSubscriptionStatus(status);
    }

    public void validatePlanStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return;
        }

        validatePlanStatus(status);
    }

    public void validateUpdateCount(int updated, String entityName) {
        if (updated == 0) {
            log.warn("Subscription validation failed. reason={}_NOT_FOUND", entityName);
            throw new IllegalArgumentException(entityName + " not found");
        }
    }

    private void validatePlanName(String name) {
        if (name == null || name.isBlank()) {
            log.warn("Subscription validation failed. reason=PLAN_NAME_REQUIRED");
            throw new IllegalArgumentException("Plan name is required");
        }

        if (name.length() > 150) {
            log.warn("Subscription validation failed. reason=PLAN_NAME_TOO_LONG name={}", name);
            throw new IllegalArgumentException("Plan name must be within 150 characters");
        }
    }

    private void validateBillingCycle(String billingCycle) {
        if (billingCycle == null || billingCycle.isBlank()) {
            log.warn("Subscription validation failed. reason=BILLING_CYCLE_REQUIRED");
            throw new IllegalArgumentException("Billing cycle is required");
        }

        if (!"MONTHLY".equalsIgnoreCase(billingCycle)
                && !"YEARLY".equalsIgnoreCase(billingCycle)
                && !"CUSTOM".equalsIgnoreCase(billingCycle)) {
            log.warn("Subscription validation failed. reason=INVALID_BILLING_CYCLE billingCycle={}", billingCycle);
            throw new IllegalArgumentException("Invalid billing cycle");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            log.warn("Subscription validation failed. reason=PRICE_REQUIRED");
            throw new IllegalArgumentException("Price is required");
        }

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Subscription validation failed. reason=NEGATIVE_PRICE price={}", price);
            throw new IllegalArgumentException("Price must be >= 0");
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            log.warn("Subscription validation failed. reason=CURRENCY_REQUIRED");
            throw new IllegalArgumentException("Currency is required");
        }

        if (currency.length() > 10) {
            log.warn("Subscription validation failed. reason=CURRENCY_TOO_LONG currency={}", currency);
            throw new IllegalArgumentException("Currency must be within 10 characters");
        }
    }

    private void validateLimits(Integer userLimit, Integer branchLimit, Integer storageLimitGb) {
        if (userLimit == null || userLimit < 1) {
            log.warn("Subscription validation failed. reason=INVALID_USER_LIMIT userLimit={}", userLimit);
            throw new IllegalArgumentException("User limit must be >= 1");
        }

        if (branchLimit == null || branchLimit < 1) {
            log.warn("Subscription validation failed. reason=INVALID_BRANCH_LIMIT branchLimit={}", branchLimit);
            throw new IllegalArgumentException("Branch limit must be >= 1");
        }

        if (storageLimitGb == null || storageLimitGb < 1) {
            log.warn("Subscription validation failed. reason=INVALID_STORAGE_LIMIT storageLimitGb={}", storageLimitGb);
            throw new IllegalArgumentException("Storage limit must be >= 1");
        }
    }

    private void validatePlanStatus(String status) {
        if (status == null || status.isBlank()) {
            log.warn("Subscription validation failed. reason=PLAN_STATUS_REQUIRED");
            throw new IllegalArgumentException("Plan status is required");
        }

        if (!"ACTIVE".equalsIgnoreCase(status)
                && !"INACTIVE".equalsIgnoreCase(status)) {
            log.warn("Subscription validation failed. reason=INVALID_PLAN_STATUS status={}", status);
            throw new IllegalArgumentException("Invalid plan status");
        }
    }

    private void validateSubscriptionStatusWithDefault(String status) {
        if (status == null || status.isBlank()) {
            return;
        }

        validateSubscriptionStatus(status);
    }

    private void validateSubscriptionStatus(String status) {
        if (!"ACTIVE".equalsIgnoreCase(status)
                && !"EXPIRED".equalsIgnoreCase(status)
                && !"CANCELLED".equalsIgnoreCase(status)
                && !"INACTIVE".equalsIgnoreCase(status)) {
            log.warn("Subscription validation failed. reason=INVALID_SUBSCRIPTION_STATUS status={}", status);
            throw new IllegalArgumentException("Invalid subscription status");
        }
    }

    private void validatePaymentStatusWithDefault(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return;
        }

        if (!"PAID".equalsIgnoreCase(paymentStatus)
                && !"UNPAID".equalsIgnoreCase(paymentStatus)
                && !"PARTIAL".equalsIgnoreCase(paymentStatus)
                && !"PENDING".equalsIgnoreCase(paymentStatus)) {
            log.warn("Subscription validation failed. reason=INVALID_PAYMENT_STATUS paymentStatus={}", paymentStatus);
            throw new IllegalArgumentException("Invalid payment status");
        }
    }
}