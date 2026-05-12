package com.plasmit.subscription.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasmit.subscription.dto.request.CreateHospitalSubscriptionRequest;
import com.plasmit.subscription.dto.request.CreatePlanRequest;
import com.plasmit.subscription.dto.request.UpdateHospitalSubscriptionRequest;
import com.plasmit.subscription.dto.request.UpdatePlanRequest;
import com.plasmit.subscription.dto.request.UpdatePlanStatusRequest;
import com.plasmit.subscription.dto.response.HospitalSubscriptionMappingResponse;
import com.plasmit.subscription.dto.response.SubscriptionPlanResponse;
import com.plasmit.subscription.repository.SubscriptionRepository;
import com.plasmit.subscription.security.TenantContext;
import com.plasmit.subscription.service.SubscriptionService;
import com.plasmit.subscription.validator.SubscriptionValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    private final SubscriptionValidator validator;

    public SubscriptionServiceImpl(
            SubscriptionRepository subscriptionRepository,
            ObjectMapper objectMapper,
            SubscriptionValidator validator
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public Map<String, Object> createPlan(CreatePlanRequest request) {

        Long userId = TenantContext.getUserId();

        validator.validateCreatePlan(request);

        log.info("Creating subscription plan. name={} createdBy={}",
                request.getName(),
                userId);

        Long planId = subscriptionRepository.createPlan(
                request,
                toJson(request.getIncludedFeatures()),
                userId
        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", planId);
        response.put("createdAt", LocalDateTime.now());

        return response;
    }

    @Override
    public List<SubscriptionPlanResponse> getPlans(String search, String status) {

        validator.validatePlanStatusFilter(status);

        log.info("Fetching subscription plans. search={} status={} userId={}",
                search,
                status,
                TenantContext.getUserId());

        return subscriptionRepository.findPlans(
                search,
                status
        );
    }

    @Override
    public Map<String, Object> updatePlan(
            Long planId,
            UpdatePlanRequest request
    ) {

        Long userId = TenantContext.getUserId();

        validator.validateUpdatePlan(
                planId,
                request
        );

        log.info("Updating subscription plan. planId={} updatedBy={}",
                planId,
                userId);

        int updated = subscriptionRepository.updatePlan(
                planId,
                request,
                toJson(request.getIncludedFeatures()),
                userId
        );

        validator.validateUpdateCount(
                updated,
                "Subscription plan"
        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", planId);
        response.put("updatedAt", LocalDateTime.now());

        return response;
    }

    @Override
    public Map<String, Object> updatePlanStatus(
            Long planId,
            UpdatePlanStatusRequest request
    ) {

        Long userId = TenantContext.getUserId();

        validator.validateUpdatePlanStatus(
                planId,
                request
        );

        log.info("Updating plan status. planId={} status={} reason={} updatedBy={}",
                planId,
                request.getStatus(),
                request.getReason(),
                userId);

        int updated = subscriptionRepository.updatePlanStatus(
                planId,
                request.getStatus(),
                userId
        );

        validator.validateUpdateCount(
                updated,
                "Subscription plan"
        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", planId);
        response.put("status", request.getStatus());
        response.put("updatedAt", LocalDateTime.now());

        return response;
    }

    @Override
    public List<HospitalSubscriptionMappingResponse> getHospitalMappings(
            Long hospitalId,
            Long planId,
            String status
    ) {

        validator.validateGetHospitalMappings(
                hospitalId,
                planId,
                status
        );

        log.info("Fetching hospital subscription mappings. hospitalId={} planId={} status={}",
                hospitalId,
                planId,
                status);

        return subscriptionRepository.findHospitalMappings(
                hospitalId,
                planId,
                status
        );
    }

    @Override
    public Map<String, Object> assignPlanToHospital(
            CreateHospitalSubscriptionRequest request
    ) {

        Long userId = TenantContext.getUserId();

        normalizeCreateMappingDefaults(request);

        validator.validateCreateHospitalMapping(request);

        log.info("Assigning plan to hospital. hospitalId={} planId={} status={} paymentStatus={} userId={}",
                request.getHospitalId(),
                request.getPlanId(),
                request.getStatus(),
                request.getPaymentStatus(),
                userId);

        Long mappingId = subscriptionRepository.createHospitalMapping(
                request,
                userId
        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", mappingId);
        response.put("hospitalId", request.getHospitalId());
        response.put("planId", request.getPlanId());
        response.put("createdAt", LocalDateTime.now());

        return response;
    }

    @Override
    public Map<String, Object> updateHospitalMapping(
            Long mappingId,
            UpdateHospitalSubscriptionRequest request
    ) {

        Long userId = TenantContext.getUserId();

        normalizeUpdateMappingDefaults(request);

        validator.validateUpdateHospitalMapping(
                mappingId,
                request
        );

        log.info("Updating hospital mapping. mappingId={} planId={} status={} paymentStatus={} userId={}",
                mappingId,
                request.getPlanId(),
                request.getStatus(),
                request.getPaymentStatus(),
                userId);

        int updated = subscriptionRepository.updateHospitalMapping(
                mappingId,
                request,
                userId
        );

        validator.validateUpdateCount(
                updated,
                "Hospital subscription mapping"
        );

        Map<String, Object> response = new HashMap<>();
        response.put("id", mappingId);
        response.put("status", request.getStatus());
        response.put("paymentStatus", request.getPaymentStatus());
        response.put("updatedAt", LocalDateTime.now());

        return response;
    }

    private void normalizeCreateMappingDefaults(
            CreateHospitalSubscriptionRequest request
    ) {

        if (request == null) {
            return;
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("ACTIVE");
        }

        if (request.getPaymentStatus() == null || request.getPaymentStatus().isBlank()) {
            request.setPaymentStatus("PENDING");
        }
    }

    private void normalizeUpdateMappingDefaults(
            UpdateHospitalSubscriptionRequest request
    ) {

        if (request == null) {
            return;
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("ACTIVE");
        }

        if (request.getPaymentStatus() == null || request.getPaymentStatus().isBlank()) {
            request.setPaymentStatus("PENDING");
        }
    }

    private String toJson(Object value) {

        try {
            return objectMapper.writeValueAsString(
                    value == null ? List.of() : value
            );
        } catch (JsonProcessingException ex) {
            log.warn("Subscription validation failed. reason=INVALID_FEATURES_JSON");
            throw new IllegalArgumentException("Invalid features JSON");
        }
    }
}