package com.plasmit.subscription.dto.response;

public class HospitalSubscriptionMappingResponse {

    private Long id;
    private Long hospitalId;
    private String hospitalName;
    private String hospitalCode;
    private Long planId;
    private String planName;
    private String subscriptionStatus;
    private String paymentStatus;
    private String renewalDate;
    private String invoiceNumber;

    public HospitalSubscriptionMappingResponse(Long id, Long hospitalId, String hospitalName,
                                               String hospitalCode, Long planId, String planName,
                                               String subscriptionStatus, String paymentStatus,
                                               String renewalDate, String invoiceNumber) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.hospitalCode = hospitalCode;
        this.planId = planId;
        this.planName = planName;
        this.subscriptionStatus = subscriptionStatus;
        this.paymentStatus = paymentStatus;
        this.renewalDate = renewalDate;
        this.invoiceNumber = invoiceNumber;
    }

    public Long getId() { return id; }
    public Long getHospitalId() { return hospitalId; }
    public String getHospitalName() { return hospitalName; }
    public String getHospitalCode() { return hospitalCode; }
    public Long getPlanId() { return planId; }
    public String getPlanName() { return planName; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getRenewalDate() { return renewalDate; }
    public String getInvoiceNumber() { return invoiceNumber; }
}