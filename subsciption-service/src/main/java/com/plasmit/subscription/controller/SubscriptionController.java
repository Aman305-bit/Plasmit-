package com.plasmit.subscription.controller;

import com.plasmit.subscription.common.ApiResponse;
import com.plasmit.subscription.dto.request.*;
import com.plasmit.subscription.dto.response.*;
import com.plasmit.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/subscriptions")
@CrossOrigin("*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public ApiResponse<List<SubscriptionPlanResponse>> getPlans(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success("Subscription plans fetched",
                subscriptionService.getPlans(search, status));
    }

    @PostMapping("/plans")
    public ApiResponse<Map<String, Object>> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        return ApiResponse.success("Subscription plan created successfully",
                subscriptionService.createPlan(request));
    }

    @PutMapping("/plans/{planId}")
    public ApiResponse<Map<String, Object>> updatePlan(
            @PathVariable Long planId,
            @Valid @RequestBody UpdatePlanRequest request
    ) {
        return ApiResponse.success("Subscription plan updated successfully",
                subscriptionService.updatePlan(planId, request));
    }

    @PatchMapping("/plans/{planId}/status")
    public ApiResponse<Map<String, Object>> updatePlanStatus(
            @PathVariable Long planId,
            @Valid @RequestBody UpdatePlanStatusRequest request
    ) {
        return ApiResponse.success("Plan status updated successfully",
                subscriptionService.updatePlanStatus(planId, request));
    }

    @GetMapping("/hospital-mappings")
    public ApiResponse<List<HospitalSubscriptionMappingResponse>> getHospitalMappings(
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) Long planId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success("Hospital subscription mappings fetched",
                subscriptionService.getHospitalMappings(hospitalId, planId, status));
    }

    @PostMapping("/hospital-mappings")
    public ApiResponse<Map<String, Object>> assignPlanToHospital(
            @Valid @RequestBody CreateHospitalSubscriptionRequest request
    ) {
        return ApiResponse.success("Subscription assigned successfully",
                subscriptionService.assignPlanToHospital(request));
    }

    @PutMapping("/hospital-mappings/{mappingId}")
    public ApiResponse<Map<String, Object>> updateHospitalMapping(
            @PathVariable Long mappingId,
            @Valid @RequestBody UpdateHospitalSubscriptionRequest request
    ) {
        return ApiResponse.success("Subscription mapping updated successfully",
                subscriptionService.updateHospitalMapping(mappingId, request));
    }
}