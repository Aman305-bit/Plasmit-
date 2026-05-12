package com.plasmit.billing.hospital.validator;

import java.math.BigDecimal;
import java.util.List;

import com.plasmit.billing.hospital.exception.ApiException;
import com.plasmit.billing.hospital.repository.BillingRepository.BillingServiceRecord;
import com.plasmit.billing.hospital.repository.BillingRepository.DashboardBootstrap;
import com.plasmit.billing.hospital.repository.BillingRepository.InvoiceItemCalculated;
import com.plasmit.billing.hospital.repository.BillingRepository.InvoiceItemRecord;
import com.plasmit.billing.hospital.repository.BillingRepository.InvoiceRecord;
import com.plasmit.billing.hospital.repository.BillingRepository.InvoiceTotals;
import com.plasmit.billing.hospital.service.BillingService.InvoiceItemRequest;
import com.plasmit.billing.hospital.service.BillingService.InvoiceRequest;
import com.plasmit.billing.hospital.service.BillingService.PaymentRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BillingValidator {

    private static final Logger log = LoggerFactory.getLogger(BillingValidator.class);

    public void validateTenantHospitalContext(Long tenantId, Long hospitalId) {

        if (tenantId == null || hospitalId == null) {
            log.warn(
                    "Billing validation failed. reason=TENANT_OR_HOSPITAL_MISSING tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (tenantId <= 0 || hospitalId <= 0) {
            log.warn(
                    "Billing validation failed. reason=INVALID_TENANT_OR_HOSPITAL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateBranchContext(Long branchId) {

        if (branchId != null && branchId <= 0) {
            log.warn("Billing validation failed. reason=INVALID_BRANCH_ID branchId={}", branchId);
            throw ApiException.badRequest("Invalid branch id.");
        }
    }

    public int validatePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public int validateSize(Integer size) {

        if (size == null || size < 1) {
            return 20;
        }

        if (size > 100) {
            log.warn("Billing validation warning. reason=PAGE_SIZE_LIMIT_EXCEEDED requestedSize={}", size);
            return 100;
        }

        return size;
    }

    public void validateInvoiceId(Long invoiceId) {

        if (invoiceId == null || invoiceId <= 0) {
            log.warn("Billing validation failed. reason=INVALID_INVOICE_ID invoiceId={}", invoiceId);
            throw ApiException.badRequest("Invalid invoice id.");
        }
    }

    public void validatePatientId(Long patientId) {

        if (patientId == null || patientId <= 0) {
            log.warn("Billing validation failed. reason=INVALID_PATIENT_ID patientId={}", patientId);
            throw ApiException.badRequest("Invalid patient id.");
        }
    }

    public void validateAppointmentId(Long appointmentId) {

        if (appointmentId != null && appointmentId <= 0) {
            log.warn("Billing validation failed. reason=INVALID_APPOINTMENT_ID appointmentId={}", appointmentId);
            throw ApiException.badRequest("Invalid appointment id.");
        }
    }

    public void validatePaymentStatusFilter(String paymentStatus) {

        if (paymentStatus == null || paymentStatus.isBlank()) {
            return;
        }

        if (!paymentStatus.equalsIgnoreCase("UNPAID")
                && !paymentStatus.equalsIgnoreCase("PARTIAL")
                && !paymentStatus.equalsIgnoreCase("PAID")) {

            log.warn("Billing validation failed. reason=INVALID_PAYMENT_STATUS paymentStatus={}", paymentStatus);
            throw ApiException.badRequest("Payment status must be UNPAID, PARTIAL or PAID.");
        }
    }

    public void validateInvoiceStatusFilter(String invoiceStatus) {

        if (invoiceStatus == null || invoiceStatus.isBlank()) {
            return;
        }

        if (!invoiceStatus.equalsIgnoreCase("ACTIVE")
                && !invoiceStatus.equalsIgnoreCase("CANCELLED")) {

            log.warn("Billing validation failed. reason=INVALID_INVOICE_STATUS invoiceStatus={}", invoiceStatus);
            throw ApiException.badRequest("Invoice status must be ACTIVE or CANCELLED.");
        }
    }

    public void validateInvoiceRequest(InvoiceRequest request) {

        if (request == null) {
            log.warn("Billing validation failed. reason=INVOICE_REQUEST_NULL");
            throw ApiException.badRequest("Invoice request is required.");
        }

        validatePatientId(request.patientId());
        validateAppointmentId(request.appointmentId());

        if (request.items() == null || request.items().isEmpty()) {
            log.warn("Billing validation failed. reason=INVOICE_ITEMS_REQUIRED patientId={}", request.patientId());
            throw ApiException.badRequest("At least one billing item is required.");
        }

        for (InvoiceItemRequest item : request.items()) {
            validateInvoiceItemRequest(item);
        }
    }

    public void validateInvoiceItemRequest(InvoiceItemRequest item) {

        if (item == null) {
            log.warn("Billing validation failed. reason=INVOICE_ITEM_NULL");
            throw ApiException.badRequest("Invoice item is required.");
        }

        if (item.serviceType() == null || item.serviceType().isBlank()) {
            log.warn("Billing validation failed. reason=SERVICE_TYPE_REQUIRED");
            throw ApiException.badRequest("Service type is required.");
        }

        String serviceType = item.serviceType().trim().toUpperCase();

        if (!serviceType.equals("DOCTOR_CONSULTATION")
                && !serviceType.equals("LAB_TEST")
                && !serviceType.equals("LAB_PACKAGE")) {

            log.warn("Billing validation failed. reason=INVALID_SERVICE_TYPE serviceType={}", item.serviceType());
            throw ApiException.badRequest("Invalid service type.");
        }

        if (item.referenceId() == null || item.referenceId() <= 0) {
            log.warn("Billing validation failed. reason=INVALID_REFERENCE_ID referenceId={}", item.referenceId());
            throw ApiException.badRequest("Invalid service reference id.");
        }

        if (item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Billing validation failed. reason=INVALID_QUANTITY quantity={}", item.quantity());
            throw ApiException.badRequest("Quantity must be greater than zero.");
        }

        if (item.discountAmount() != null && item.discountAmount().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Billing validation failed. reason=NEGATIVE_DISCOUNT discountAmount={}", item.discountAmount());
            throw ApiException.badRequest("Discount cannot be negative.");
        }
    }

    public void validatePatientExists(
            boolean exists,
            Long patientId,
            Long tenantId,
            Long hospitalId
    ) {

        if (!exists) {
            log.warn(
                    "Billing validation failed. reason=PATIENT_NOT_FOUND patientId={} tenantId={} hospitalId={}",
                    patientId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Patient not found.");
        }
    }

    public void validateAppointmentForPatient(
            boolean exists,
            Long appointmentId,
            Long patientId
    ) {

        if (!exists) {
            log.warn(
                    "Billing validation failed. reason=APPOINTMENT_NOT_FOUND_FOR_PATIENT appointmentId={} patientId={}",
                    appointmentId,
                    patientId
            );
            throw ApiException.notFound("Appointment not found for selected patient.");
        }
    }

    public void validateBillingServiceFound(
            BillingServiceRecord service,
            String serviceType,
            Long referenceId
    ) {

        if (service == null) {
            log.warn(
                    "Billing validation failed. reason=BILLING_SERVICE_NOT_FOUND serviceType={} referenceId={}",
                    serviceType,
                    referenceId
            );
            throw ApiException.notFound("Billing service not found: " + serviceType);
        }
    }

    public void validateDiscountWithinLineAmount(
            BigDecimal discountAmount,
            BigDecimal grossLineAmount
    ) {

        BigDecimal safeDiscount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal safeGross = grossLineAmount == null ? BigDecimal.ZERO : grossLineAmount;

        if (safeDiscount.compareTo(safeGross) > 0) {
            log.warn(
                    "Billing validation failed. reason=DISCOUNT_GREATER_THAN_LINE_AMOUNT discount={} gross={}",
                    safeDiscount,
                    safeGross
            );
            throw ApiException.badRequest("Discount cannot be greater than line amount.");
        }
    }

    public void validateCalculatedItems(List<InvoiceItemCalculated> items) {

        if (items == null || items.isEmpty()) {
            log.warn("Billing validation failed. reason=CALCULATED_ITEMS_EMPTY");
            throw ApiException.badRequest("At least one billing item is required.");
        }
    }

    public void validateInvoiceTotals(InvoiceTotals totals) {

        if (totals == null) {
            log.warn("Billing validation failed. reason=INVOICE_TOTALS_NULL");
            throw ApiException.badRequest("Invoice totals could not be calculated.");
        }

        if (totals.netAmount() == null || totals.netAmount().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Billing validation failed. reason=INVALID_NET_AMOUNT netAmount={}",
                    totals.netAmount());
            throw ApiException.badRequest("Invalid invoice amount.");
        }
    }

    public void validateInvoiceFound(
            InvoiceRecord invoice,
            Long invoiceId,
            Long tenantId,
            Long hospitalId
    ) {

        if (invoice == null) {
            log.warn(
                    "Billing validation failed. reason=INVOICE_NOT_FOUND invoiceId={} tenantId={} hospitalId={}",
                    invoiceId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Invoice not found.");
        }
    }

    public void validateInvoiceItems(
            List<InvoiceItemRecord> items,
            Long invoiceId
    ) {

        if (items == null) {
            log.warn("Billing validation failed. reason=INVOICE_ITEM_LIST_NULL invoiceId={}", invoiceId);
            throw ApiException.badRequest("Unable to fetch invoice items.");
        }
    }

    public void validateInvoiceList(
            List<InvoiceRecord> invoices,
            Long tenantId,
            Long hospitalId
    ) {

        if (invoices == null) {
            log.warn(
                    "Billing validation failed. reason=INVOICE_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch invoices.");
        }
    }

    public void validateBillingServices(
            List<BillingServiceRecord> services,
            Long tenantId,
            Long hospitalId
    ) {

        if (services == null) {
            log.warn(
                    "Billing validation failed. reason=BILLING_SERVICE_LIST_NULL tenantId={} hospitalId={}",
                    tenantId,
                    hospitalId
            );
            throw ApiException.badRequest("Unable to fetch billing services.");
        }
    }

    public void validateDashboardBootstrap(DashboardBootstrap bootstrap) {

        if (bootstrap == null) {
            log.warn("Billing validation failed. reason=DASHBOARD_BOOTSTRAP_NULL");
            throw ApiException.badRequest("Unable to fetch billing dashboard.");
        }
    }

    public void validatePaymentRequest(PaymentRequest request) {

        if (request == null) {
            log.warn("Billing validation failed. reason=PAYMENT_REQUEST_NULL");
            throw ApiException.badRequest("Payment request is required.");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Billing validation failed. reason=INVALID_PAYMENT_AMOUNT amount={}",
                    request.amount());
            throw ApiException.badRequest("Payment amount must be greater than zero.");
        }

        validatePaymentMethod(request.paymentMethod());
    }

    public void validatePaymentMethod(String paymentMethod) {

        if (paymentMethod == null || paymentMethod.isBlank()) {
            return;
        }

        String value = paymentMethod.trim().toUpperCase();

        if (!value.equals("CASH")
                && !value.equals("UPI")
                && !value.equals("CARD")
                && !value.equals("BANK_TRANSFER")
                && !value.equals("CHEQUE")) {

            log.warn("Billing validation failed. reason=INVALID_PAYMENT_METHOD paymentMethod={}", paymentMethod);
            throw ApiException.badRequest("Invalid payment method.");
        }
    }

    public void validateInvoiceCanAcceptPayment(InvoiceRecord invoice) {

        if ("CANCELLED".equalsIgnoreCase(invoice.invoiceStatus())) {
            log.warn("Billing validation failed. reason=PAYMENT_ON_CANCELLED_INVOICE invoiceId={}", invoice.id());
            throw ApiException.badRequest("Payment cannot be added to cancelled invoice.");
        }

        if (invoice.balanceAmount() == null || invoice.balanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Billing validation failed. reason=NO_BALANCE_DUE invoiceId={} balance={}",
                    invoice.id(),
                    invoice.balanceAmount());
            throw ApiException.badRequest("Invoice has no pending balance.");
        }
    }

    public void validatePaymentAmountWithinBalance(
            BigDecimal paymentAmount,
            BigDecimal balanceAmount
    ) {

        if (paymentAmount.compareTo(balanceAmount) > 0) {
            log.warn(
                    "Billing validation failed. reason=PAYMENT_GREATER_THAN_BALANCE paymentAmount={} balanceAmount={}",
                    paymentAmount,
                    balanceAmount
            );
            throw ApiException.badRequest("Payment amount cannot be greater than invoice balance.");
        }
    }

    public void validateInvoiceCanCancel(InvoiceRecord invoice) {

        if ("CANCELLED".equalsIgnoreCase(invoice.invoiceStatus())) {
            log.warn("Billing validation failed. reason=INVOICE_ALREADY_CANCELLED invoiceId={}", invoice.id());
            throw ApiException.badRequest("Invoice is already cancelled.");
        }
    }

    public void validateCancelCount(
            int updated,
            Long invoiceId,
            Long tenantId,
            Long hospitalId
    ) {

        if (updated == 0) {
            log.warn(
                    "Billing validation failed. reason=INVOICE_CANCEL_FAILED invoiceId={} tenantId={} hospitalId={}",
                    invoiceId,
                    tenantId,
                    hospitalId
            );
            throw ApiException.notFound("Invoice not found.");
        }
    }
}