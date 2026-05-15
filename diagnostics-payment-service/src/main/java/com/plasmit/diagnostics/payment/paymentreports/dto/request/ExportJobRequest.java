package com.plasmit.diagnostics.payment.paymentreports.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;

public class ExportJobRequest {

    @NotBlank(message = "reportType is required.")
    private String reportType;

    @NotBlank(message = "format is required.")
    private String format;

    @NotNull(message = "fromDate is required.")
    private LocalDate fromDate;

    @NotNull(message = "toDate is required.")
    private LocalDate toDate;

    private Map<String, Object> filters;

    @Size(max = 500, message = "notes cannot exceed 500 characters.")
    private String notes;

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}