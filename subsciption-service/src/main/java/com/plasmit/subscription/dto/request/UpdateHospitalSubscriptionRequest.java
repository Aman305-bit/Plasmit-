package com.plasmit.subscription.dto.request;

import jakarta.validation.constraints.NotNull;

public class UpdateHospitalSubscriptionRequest {

    @NotNull private Long planId;
    private String status;
    private String paymentStatus;
    private String reason;

    public Long getPlanId() { return planId; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getReason() { return reason; }

    public void setPlanId(Long planId) { this.planId = planId; }
    public void setStatus(String status) { this.status = status; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setReason(String reason) { this.reason = reason; }
}