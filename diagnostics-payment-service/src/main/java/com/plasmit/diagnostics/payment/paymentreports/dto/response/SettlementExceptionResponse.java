package com.plasmit.diagnostics.payment.paymentreports.dto.response;

import java.time.LocalDateTime;

public class SettlementExceptionResponse {

    private Long id;
    private String invoiceNo;
    private String paymentMethod;
    private Long expectedPaise;
    private Long settledPaise;
    private Long differencePaise;
    private String reason;
    private String severity;
    private String owner;
    private LocalDateTime dueAt;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getExpectedPaise() {
        return expectedPaise;
    }

    public void setExpectedPaise(Long expectedPaise) {
        this.expectedPaise = expectedPaise;
    }

    public Long getSettledPaise() {
        return settledPaise;
    }

    public void setSettledPaise(Long settledPaise) {
        this.settledPaise = settledPaise;
    }

    public Long getDifferencePaise() {
        return differencePaise;
    }

    public void setDifferencePaise(Long differencePaise) {
        this.differencePaise = differencePaise;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}