package com.plasmit.diagnostics.payment.paymentreports.dto.response;

public class DepartmentBillingResponse {

    private String department;
    private Long grossPaise;
    private Long netPaise;
    private Long collectedPaise;
    private Long duePaise;
    private Long refundPaise;
    private Long invoiceCount;

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getGrossPaise() {
        return grossPaise;
    }

    public void setGrossPaise(Long grossPaise) {
        this.grossPaise = grossPaise;
    }

    public Long getNetPaise() {
        return netPaise;
    }

    public void setNetPaise(Long netPaise) {
        this.netPaise = netPaise;
    }

    public Long getCollectedPaise() {
        return collectedPaise;
    }

    public void setCollectedPaise(Long collectedPaise) {
        this.collectedPaise = collectedPaise;
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

    public Long getInvoiceCount() {
        return invoiceCount;
    }

    public void setInvoiceCount(Long invoiceCount) {
        this.invoiceCount = invoiceCount;
    }
}