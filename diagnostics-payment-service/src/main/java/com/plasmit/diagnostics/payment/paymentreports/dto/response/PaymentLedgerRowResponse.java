package com.plasmit.diagnostics.payment.paymentreports.dto.response;

import java.time.LocalDateTime;

public class PaymentLedgerRowResponse {

    private Long invoiceId;
    private String invoiceNo;
    private LocalDateTime billDateTime;

    private String branchId;

    private String patientUhid;
    private String patientName;
    private String patientAgeGender;

    private Long encounterId;
    private String encounterType;

    private String payerType;
    private String payerName;
    private String authorizationNo;

    private String department;
    private String serviceName;

    private Long grossPaise;
    private Long discountPaise;
    private Long taxPaise;
    private Long netPaise;
    private Long paidPaise;
    private Long duePaise;
    private Long refundPaise;

    private String paymentMethod;
    private String status;

    private Long cashierId;
    private String cashierName;

    private String settlementStatus;
    private String settlementBatchId;
    private LocalDateTime settledAt;

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public LocalDateTime getBillDateTime() {
        return billDateTime;
    }

    public void setBillDateTime(LocalDateTime billDateTime) {
        this.billDateTime = billDateTime;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getPatientUhid() {
        return patientUhid;
    }

    public void setPatientUhid(String patientUhid) {
        this.patientUhid = patientUhid;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientAgeGender() {
        return patientAgeGender;
    }

    public void setPatientAgeGender(String patientAgeGender) {
        this.patientAgeGender = patientAgeGender;
    }

    public Long getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(Long encounterId) {
        this.encounterId = encounterId;
    }

    public String getEncounterType() {
        return encounterType;
    }

    public void setEncounterType(String encounterType) {
        this.encounterType = encounterType;
    }

    public String getPayerType() {
        return payerType;
    }

    public void setPayerType(String payerType) {
        this.payerType = payerType;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public String getAuthorizationNo() {
        return authorizationNo;
    }

    public void setAuthorizationNo(String authorizationNo) {
        this.authorizationNo = authorizationNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getGrossPaise() {
        return grossPaise;
    }

    public void setGrossPaise(Long grossPaise) {
        this.grossPaise = grossPaise;
    }

    public Long getDiscountPaise() {
        return discountPaise;
    }

    public void setDiscountPaise(Long discountPaise) {
        this.discountPaise = discountPaise;
    }

    public Long getTaxPaise() {
        return taxPaise;
    }

    public void setTaxPaise(Long taxPaise) {
        this.taxPaise = taxPaise;
    }

    public Long getNetPaise() {
        return netPaise;
    }

    public void setNetPaise(Long netPaise) {
        this.netPaise = netPaise;
    }

    public Long getPaidPaise() {
        return paidPaise;
    }

    public void setPaidPaise(Long paidPaise) {
        this.paidPaise = paidPaise;
    }

    public Long getDuePaise() {
        return duePaise;
    }

    public void setDuePaise(Long duePaise) {
        this.duePaise = duePaise;
    }

    public Long getRefundPaise() {
        return refundPaise;
    }

    public void setRefundPaise(Long refundPaise) {
        this.refundPaise = refundPaise;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public void setCashierId(Long cashierId) {
        this.cashierId = cashierId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getSettlementBatchId() {
        return settlementBatchId;
    }

    public void setSettlementBatchId(String settlementBatchId) {
        this.settlementBatchId = settlementBatchId;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }
}