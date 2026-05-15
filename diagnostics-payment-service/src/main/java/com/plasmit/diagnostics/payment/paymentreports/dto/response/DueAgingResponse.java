package com.plasmit.diagnostics.payment.paymentreports.dto.response;

public class DueAgingResponse {

    private String payerType;
    private Long currentPaise;
    private Long days1To7Paise;
    private Long days8To30Paise;
    private Long days31To60Paise;
    private Long days60PlusPaise;
    private Long invoiceCount;

    public String getPayerType() {
        return payerType;
    }

    public void setPayerType(String payerType) {
        this.payerType = payerType;
    }

    public Long getCurrentPaise() {
        return currentPaise;
    }

    public void setCurrentPaise(Long currentPaise) {
        this.currentPaise = currentPaise;
    }

    public Long getDays1To7Paise() {
        return days1To7Paise;
    }

    public void setDays1To7Paise(Long days1To7Paise) {
        this.days1To7Paise = days1To7Paise;
    }

    public Long getDays8To30Paise() {
        return days8To30Paise;
    }

    public void setDays8To30Paise(Long days8To30Paise) {
        this.days8To30Paise = days8To30Paise;
    }

    public Long getDays31To60Paise() {
        return days31To60Paise;
    }

    public void setDays31To60Paise(Long days31To60Paise) {
        this.days31To60Paise = days31To60Paise;
    }

    public Long getDays60PlusPaise() {
        return days60PlusPaise;
    }

    public void setDays60PlusPaise(Long days60PlusPaise) {
        this.days60PlusPaise = days60PlusPaise;
    }

    public Long getInvoiceCount() {
        return invoiceCount;
    }

    public void setInvoiceCount(Long invoiceCount) {
        this.invoiceCount = invoiceCount;
    }
}