package com.plasmit.diagnostics.payment.paymentreports.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class ReconciliationRunRequest {

    @NotNull(message = "fromDate is required.")
    private LocalDate fromDate;

    @NotNull(message = "toDate is required.")
    private LocalDate toDate;

    @NotEmpty(message = "channels are required.")
    private List<String> channels;

    @Size(max = 500, message = "notes cannot exceed 500 characters.")
    private String notes;

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

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}