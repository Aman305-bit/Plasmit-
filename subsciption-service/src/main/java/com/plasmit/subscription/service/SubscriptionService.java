package com.plasmit.subscription.service;

import com.plasmit.subscription.dto.request.*;
import com.plasmit.subscription.dto.response.*;

import java.util.List;
import java.util.Map;

public interface SubscriptionService {

    Map<String, Object> createPlan(CreatePlanRequest request);

    List<SubscriptionPlanResponse> getPlans(String search, String status);

    Map<String, Object> updatePlan(Long planId, UpdatePlanRequest request);

    Map<String, Object> updatePlanStatus(Long planId, UpdatePlanStatusRequest request);

    List<HospitalSubscriptionMappingResponse> getHospitalMappings(Long hospitalId, Long planId, String status);

    Map<String, Object> assignPlanToHospital(CreateHospitalSubscriptionRequest request);

    Map<String, Object> updateHospitalMapping(Long mappingId, UpdateHospitalSubscriptionRequest request);
}