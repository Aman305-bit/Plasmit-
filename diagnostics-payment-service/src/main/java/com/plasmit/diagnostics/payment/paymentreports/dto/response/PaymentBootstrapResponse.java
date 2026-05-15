package com.plasmit.diagnostics.payment.paymentreports.dto.response;

import java.util.List;

public class PaymentBootstrapResponse {

    private List<PaymentFilterOptionResponse> branches;
    private List<String> departments;
    private List<String> payerTypes;
    private List<String> paymentMethods;
    private List<String> statuses;
    private List<String> settlementStatuses;
    private List<PaymentFilterOptionResponse> cashiers;

    public List<PaymentFilterOptionResponse> getBranches() {
        return branches;
    }

    public void setBranches(List<PaymentFilterOptionResponse> branches) {
        this.branches = branches;
    }

    public List<String> getDepartments() {
        return departments;
    }

    public void setDepartments(List<String> departments) {
        this.departments = departments;
    }

    public List<String> getPayerTypes() {
        return payerTypes;
    }

    public void setPayerTypes(List<String> payerTypes) {
        this.payerTypes = payerTypes;
    }

    public List<String> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<String> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public List<String> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<String> statuses) {
        this.statuses = statuses;
    }

    public List<String> getSettlementStatuses() {
        return settlementStatuses;
    }

    public void setSettlementStatuses(List<String> settlementStatuses) {
        this.settlementStatuses = settlementStatuses;
    }

    public List<PaymentFilterOptionResponse> getCashiers() {
        return cashiers;
    }

    public void setCashiers(List<PaymentFilterOptionResponse> cashiers) {
        this.cashiers = cashiers;
    }
}