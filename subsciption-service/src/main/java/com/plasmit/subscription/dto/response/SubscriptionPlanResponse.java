package com.plasmit.subscription.dto.response;

import java.math.BigDecimal;

public class SubscriptionPlanResponse {

    private Long id;
    private String name;
    private String billingCycle;
    private BigDecimal price;
    private String currency;
    private Integer userLimit;
    private Integer branchLimit;
    private Integer storageLimitGb;
    private String includedFeatures;
    private String status;
    private String createdAt;

    public SubscriptionPlanResponse(Long id, String name, String billingCycle, BigDecimal price,
                                    String currency, Integer userLimit, Integer branchLimit,
                                    Integer storageLimitGb, String includedFeatures,
                                    String status, String createdAt) {
        this.id = id;
        this.name = name;
        this.billingCycle = billingCycle;
        this.price = price;
        this.currency = currency;
        this.userLimit = userLimit;
        this.branchLimit = branchLimit;
        this.storageLimitGb = storageLimitGb;
        this.includedFeatures = includedFeatures;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBillingCycle() { return billingCycle; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public Integer getUserLimit() { return userLimit; }
    public Integer getBranchLimit() { return branchLimit; }
    public Integer getStorageLimitGb() { return storageLimitGb; }
    public String getIncludedFeatures() { return includedFeatures; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}