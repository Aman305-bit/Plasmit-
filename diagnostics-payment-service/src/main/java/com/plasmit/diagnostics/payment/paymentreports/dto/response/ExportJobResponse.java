package com.plasmit.diagnostics.payment.paymentreports.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class ExportJobResponse {

    private Long id;
    private String exportCode;

    private String reportType;
    private String format;

    private LocalDate fromDate;
    private LocalDate toDate;

    private Map<String, Object> filters;

    private String status;
    private String fileUrl;
    private String signedDownloadUrl;
    private LocalDateTime expiresAt;

    private Long createdBy;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getExportCode() {
        return exportCode;
    }

    public String getReportType() {
        return reportType;
    }

    public String getFormat() {
        return format;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public String getStatus() {
        return status;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getSignedDownloadUrl() {
        return signedDownloadUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setExportCode(String exportCode) {
        this.exportCode = exportCode;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setSignedDownloadUrl(String signedDownloadUrl) {
        this.signedDownloadUrl = signedDownloadUrl;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}