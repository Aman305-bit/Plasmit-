package com.plasmit.diagnostics.payment.paymentreports.dto.response;

public class PaymentSummaryResponse {

    private Long grossBillingPaise;
    private Long discountPaise;
    private Long taxPaise;
    private Long netBillingPaise;
    private Long collectedPaise;
    private Long outstandingPaise;
    private Long refundPaise;
    private Long invoiceCount;
    private Long paidInvoiceCount;
    private Long partialInvoiceCount;
    private Long dueInvoiceCount;

    public Long getGrossBillingPaise() {
        return grossBillingPaise;
    }

    public void setGrossBillingPaise(Long grossBillingPaise) {
        this.grossBillingPaise = grossBillingPaise;
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

    public Long getNetBillingPaise() {
        return netBillingPaise;
    }

    public void setNetBillingPaise(Long netBillingPaise) {
        this.netBillingPaise = netBillingPaise;
    }

    public Long getCollectedPaise() {
        return collectedPaise;
    }

    public void setCollectedPaise(Long collectedPaise) {
        this.collectedPaise = collectedPaise;
    }

    public Long getOutstandingPaise() {
        return outstandingPaise;
    }

    public void setOutstandingPaise(Long outstandingPaise) {
        this.outstandingPaise = outstandingPaise;
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

    public Long getPaidInvoiceCount() {
        return paidInvoiceCount;
    }

    public void setPaidInvoiceCount(Long paidInvoiceCount) {
        this.paidInvoiceCount = paidInvoiceCount;
    }

    public Long getPartialInvoiceCount() {
        return partialInvoiceCount;
    }

    public void setPartialInvoiceCount(Long partialInvoiceCount) {
        this.partialInvoiceCount = partialInvoiceCount;
    }

    public Long getDueInvoiceCount() {
        return dueInvoiceCount;
    }

    public void setDueInvoiceCount(Long dueInvoiceCount) {
        this.dueInvoiceCount = dueInvoiceCount;
    }
}