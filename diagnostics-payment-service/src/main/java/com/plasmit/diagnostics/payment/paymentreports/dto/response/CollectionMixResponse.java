package com.plasmit.diagnostics.payment.paymentreports.dto.response;

public class CollectionMixResponse {

    private String method;
    private Long collectedPaise;
    private Long transactionCount;
    private Double percentage;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Long getCollectedPaise() {
        return collectedPaise;
    }

    public void setCollectedPaise(Long collectedPaise) {
        this.collectedPaise = collectedPaise;
    }

    public Long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }
}