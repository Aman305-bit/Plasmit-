package com.plasmit.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CreatePlanRequest {

    @NotBlank private String name;
    @NotBlank private String billingCycle;
    @NotNull private BigDecimal price;

    private String currency = "INR";

    @NotNull private Integer userLimit;
    @NotNull private Integer branchLimit;
    @NotNull private Integer storageLimitGb;

    private List<String> includedFeatures;

    public String getName() { return name; }
    public String getBillingCycle() { return billingCycle; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public Integer getUserLimit() { return userLimit; }
    public Integer getBranchLimit() { return branchLimit; }
    public Integer getStorageLimitGb() { return storageLimitGb; }
    public List<String> getIncludedFeatures() { return includedFeatures; }

    public void setName(String name) { this.name = name; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setUserLimit(Integer userLimit) { this.userLimit = userLimit; }
    public void setBranchLimit(Integer branchLimit) { this.branchLimit = branchLimit; }
    public void setStorageLimitGb(Integer storageLimitGb) { this.storageLimitGb = storageLimitGb; }
    public void setIncludedFeatures(List<String> includedFeatures) { this.includedFeatures = includedFeatures; }
}