package com.plasmit.subscription.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateHospitalSubscriptionRequest {

    @NotNull private Long hospitalId;
    @NotNull private Long planId;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;

    private String status = "ACTIVE";
    private String paymentStatus = "PENDING";
    private String invoiceNumber;

    public Long getHospitalId() { return hospitalId; }
    public Long getPlanId() { return planId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getInvoiceNumber() { return invoiceNumber; }

    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setStatus(String status) { this.status = status; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
}