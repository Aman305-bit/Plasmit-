package com.plasmit.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;

public class UpdatePlanStatusRequest {

    @NotBlank
    private String status;

    private String reason;

    public String getStatus() { return status; }
    public String getReason() { return reason; }

    public void setStatus(String status) { this.status = status; }
    public void setReason(String reason) { this.reason = reason; }
}