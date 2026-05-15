package com.plasmit.diagnostics.payment.paymentreports.validator;

import com.plasmit.diagnostics.payment.common.exception.ApiException;
import com.plasmit.diagnostics.payment.paymentreports.dto.request.ExportJobRequest;
import com.plasmit.diagnostics.payment.paymentreports.dto.request.ReconciliationRunRequest;
import com.plasmit.diagnostics.payment.validator.DateRangeValidator;
import com.plasmit.diagnostics.payment.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
public class PaymentReportValidator {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "billDateTime",
            "invoiceNo",
            "patientName",
            "department",
            "payerType",
            "paymentMethod",
            "status",
            "settlementStatus",
            "netPaise",
            "paidPaise",
            "duePaise"
    );

    private static final List<String> ALLOWED_RECONCILIATION_CHANNELS = List.of(
            "Cash",
            "UPI",
            "Card",
            "Insurance",
            "TPA",
            "Corporate",
            "Bank",
            "Gateway"
    );

    private static final List<String> ALLOWED_EXPORT_REPORT_TYPES = List.of(
            "Ledger",
            "SettlementExceptions",
            "DepartmentBilling",
            "DueAging",
            "Audit"
    );

    private static final List<String> ALLOWED_EXPORT_FORMATS = List.of(
            "XLSX",
            "CSV",
            "PDF"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public PaymentReportValidator(DateRangeValidator dateRangeValidator,
                                  PaginationValidator paginationValidator) {
        this.dateRangeValidator = dateRangeValidator;
        this.paginationValidator = paginationValidator;
    }

    public void validateSummaryRequest(LocalDate fromDate, LocalDate toDate) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
    }

    public void validateBootstrapRequest(LocalDate fromDate, LocalDate toDate) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
    }

    public void validateCollectionMixRequest(LocalDate fromDate, LocalDate toDate) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
    }

    public void validateDepartmentBillingRequest(LocalDate fromDate, LocalDate toDate) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
    }

    public void validateDueAgingRequest(LocalDate fromDate, LocalDate toDate) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
    }

    public void validateSettlementExceptionRequest(LocalDate fromDate, LocalDate toDate) {
        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
    }

    public void validateLedgerRequest(LocalDate fromDate,
                                      LocalDate toDate,
                                      Integer page,
                                      Integer limit,
                                      String sortBy,
                                      String sortOrder) {

        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
        paginationValidator.validate(page, limit);

        if (sortBy != null && !sortBy.isBlank() && !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw ApiException.validation("Invalid sortBy value.");
        }

        if (sortOrder != null
                && !sortOrder.isBlank()
                && !sortOrder.equalsIgnoreCase("asc")
                && !sortOrder.equalsIgnoreCase("desc")) {
            throw ApiException.validation("sortOrder must be asc or desc.");
        }
    }

    public void validateReconciliationRunRequest(ReconciliationRunRequest request) {

        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        dateRangeValidator.validateMaxRangeDays(request.getFromDate(), request.getToDate(), 31);

        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            throw ApiException.validation("At least one reconciliation channel is required.");
        }

        for (String channel : request.getChannels()) {
            if (channel == null || channel.isBlank()) {
                throw ApiException.validation("Reconciliation channel cannot be blank.");
            }

            if (!ALLOWED_RECONCILIATION_CHANNELS.contains(channel)) {
                throw ApiException.validation("Invalid reconciliation channel: " + channel);
            }
        }
    }

    public void validateExportJobRequest(ExportJobRequest request) {

        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        dateRangeValidator.validateMaxRangeDays(request.getFromDate(), request.getToDate(), 366);

        if (request.getReportType() == null || request.getReportType().isBlank()) {
            throw ApiException.validation("reportType is required.");
        }

        if (!ALLOWED_EXPORT_REPORT_TYPES.contains(request.getReportType())) {
            throw ApiException.validation("Invalid reportType.");
        }

        if (request.getFormat() == null || request.getFormat().isBlank()) {
            throw ApiException.validation("format is required.");
        }

        if (!ALLOWED_EXPORT_FORMATS.contains(request.getFormat())) {
            throw ApiException.validation("Invalid export format.");
        }
    }
    public void validateAuditRequest(LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer limit) {

dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
paginationValidator.validate(page, limit);
}
}