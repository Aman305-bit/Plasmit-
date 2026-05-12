package com.plasmit.billing.hospital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.plasmit.billing.hospital.exception.ApiException;
import com.plasmit.billing.hospital.repository.BillingRepository;
import com.plasmit.billing.hospital.repository.BillingRepository.BillingServiceRecord;
import com.plasmit.billing.hospital.repository.BillingRepository.DashboardBootstrap;
import com.plasmit.billing.hospital.repository.BillingRepository.InvoiceItemCalculated;
import com.plasmit.billing.hospital.repository.BillingRepository.InvoiceItemRecord;
import com.plasmit.billing.hospital.repository.BillingRepository.InvoiceRecord;
import com.plasmit.billing.hospital.repository.BillingRepository.InvoiceTotals;
import com.plasmit.billing.hospital.security.SecurityConfig.CurrentUser;
import com.plasmit.billing.hospital.security.SecurityConfig.TenantContext;
import com.plasmit.billing.hospital.validator.BillingValidator;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingRepository repository;
    private final BillingValidator validator;

    public BillingService(
            BillingRepository repository,
            BillingValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    public DashboardBootstrap bootstrap() {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());

        requirePermission(currentUser, "billing.view");

        log.info("Billing bootstrap request. tenantId={} hospitalId={} branchId={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId());

        DashboardBootstrap bootstrap = repository.bootstrap(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        );

        validator.validateDashboardBootstrap(bootstrap);

        return bootstrap;
    }

    public List<BillingServiceRecord> billingServices(String query) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());

        requirePermission(currentUser, "billing.view");

        log.info("Billing services request. tenantId={} hospitalId={} branchId={} query={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query);

        List<BillingServiceRecord> services = repository.findBillingServices(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                query
        );

        validator.validateBillingServices(
                services,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return services;
    }

    public BillingPreviewResponse previewInvoice(InvoiceRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateInvoiceRequest(request);

        requirePermission(currentUser, "billing.view");

        validatePatientAndAppointment(
                currentUser,
                request.patientId(),
                request.appointmentId()
        );

        List<InvoiceItemCalculated> items = calculateItems(
                currentUser,
                request.items()
        );

        validator.validateCalculatedItems(items);

        InvoiceTotals totals = calculateTotals(items);

        validator.validateInvoiceTotals(totals);

        log.info("Invoice preview calculated. patientId={} itemCount={} gross={} net={}",
                request.patientId(),
                items.size(),
                totals.grossAmount(),
                totals.netAmount());

        return new BillingPreviewResponse(
                request.patientId(),
                request.appointmentId(),
                items,
                totals,
                request.notes()
        );
    }

    @Transactional
    public InvoiceDetailResponse createInvoice(InvoiceRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateInvoiceRequest(request);

        requirePermission(currentUser, "billing.create");

        log.info("Create invoice request. tenantId={} hospitalId={} patientId={} itemCount={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.patientId(),
                request.items() == null ? 0 : request.items().size());

        validatePatientAndAppointment(
                currentUser,
                request.patientId(),
                request.appointmentId()
        );

        List<InvoiceItemCalculated> items = calculateItems(
                currentUser,
                request.items()
        );

        validator.validateCalculatedItems(items);

        InvoiceTotals totals = calculateTotals(items);

        validator.validateInvoiceTotals(totals);

        String invoiceNumber = repository.generateNextInvoiceNumber(currentUser.getHospitalId());

        Long invoiceId = repository.createInvoice(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getDepartmentId(),
                currentUser.getUserId(),
                invoiceNumber,
                request.patientId(),
                request.appointmentId(),
                totals,
                request.notes()
        );

        for (InvoiceItemCalculated item : items) {
            repository.createInvoiceItem(
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    currentUser.getBranchId(),
                    invoiceId,
                    item
            );
        }

        repository.saveStatusHistory(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                invoiceId,
                null,
                "ACTIVE",
                "Invoice created.",
                currentUser.getUserId()
        );

        log.info("Invoice created successfully. invoiceId={} invoiceNumber={} netAmount={}",
                invoiceId,
                invoiceNumber,
                totals.netAmount());

        return getInvoice(invoiceId);
    }

    public List<InvoiceRecord> listInvoices(
            String paymentStatus,
            String invoiceStatus,
            Long patientId,
            Integer page,
            Integer size
    ) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validatePaymentStatusFilter(paymentStatus);
        validator.validateInvoiceStatusFilter(invoiceStatus);

        if (patientId != null) {
            validator.validatePatientId(patientId);
        }

        requirePermission(currentUser, "billing.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        log.info("List invoices request. tenantId={} hospitalId={} paymentStatus={} invoiceStatus={} page={} size={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                paymentStatus,
                invoiceStatus,
                safePage,
                safeSize);

        List<InvoiceRecord> invoices = repository.findInvoices(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                paymentStatus,
                invoiceStatus,
                patientId,
                safeSize,
                offset
        );

        validator.validateInvoiceList(
                invoices,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return invoices;
    }

    public InvoiceDetailResponse getInvoice(Long invoiceId) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateInvoiceId(invoiceId);

        requirePermission(currentUser, "billing.view");

        InvoiceRecord invoice = repository.findInvoiceById(
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateInvoiceFound(
                invoice,
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<InvoiceItemRecord> items = repository.findInvoiceItems(
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateInvoiceItems(
                items,
                invoiceId
        );

        return new InvoiceDetailResponse(
                invoice,
                items
        );
    }

    @Transactional
    public InvoiceDetailResponse addPayment(Long invoiceId, PaymentRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateInvoiceId(invoiceId);
        validator.validatePaymentRequest(request);

        requirePermission(currentUser, "billing.create");

        InvoiceRecord invoice = repository.findInvoiceById(
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateInvoiceFound(
                invoice,
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateInvoiceCanAcceptPayment(invoice);

        BigDecimal amountToApply = request.amount();

        validator.validatePaymentAmountWithinBalance(
                amountToApply,
                invoice.balanceAmount()
        );

        String paymentNumber = repository.generateNextPaymentNumber(currentUser.getHospitalId());

        Long paymentId = repository.createPayment(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                paymentNumber,
                invoice.patientId(),
                invoiceId,
                amountToApply,
                emptyDefault(request.paymentMethod(), "CASH"),
                request.transactionReference(),
                request.remarks()
        );

        repository.createPaymentAllocation(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                paymentId,
                invoiceId,
                amountToApply
        );

        BigDecimal newPaidAmount = invoice.paidAmount().add(amountToApply);
        BigDecimal newBalanceAmount = invoice.netAmount().subtract(newPaidAmount);

        if (newBalanceAmount.compareTo(BigDecimal.ZERO) < 0) {
            newBalanceAmount = BigDecimal.ZERO;
        }

        String newPaymentStatus = resolvePaymentStatus(
                invoice.netAmount(),
                newPaidAmount
        );

        repository.updateInvoicePaymentTotals(
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                newPaidAmount,
                newBalanceAmount,
                newPaymentStatus,
                currentUser.getUserId()
        );

        repository.saveStatusHistory(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                invoiceId,
                invoice.paymentStatus(),
                newPaymentStatus,
                "Payment received: " + amountToApply,
                currentUser.getUserId()
        );

        log.info("Payment added successfully. invoiceId={} paymentId={} amount={} newStatus={}",
                invoiceId,
                paymentId,
                amountToApply,
                newPaymentStatus);

        return getInvoice(invoiceId);
    }

    @Transactional
    public Map<String, Object> cancelInvoice(Long invoiceId, CancelRequest request) {

        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateInvoiceId(invoiceId);

        requirePermission(currentUser, "billing.refund");

        InvoiceRecord invoice = repository.findInvoiceById(
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateInvoiceFound(
                invoice,
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateInvoiceCanCancel(invoice);

        String remarks = request == null || request.remarks() == null || request.remarks().isBlank()
                ? "Invoice cancelled."
                : request.remarks();

        int updated = repository.cancelInvoice(
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                remarks
        );

        validator.validateCancelCount(
                updated,
                invoiceId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        repository.saveStatusHistory(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                invoiceId,
                invoice.invoiceStatus(),
                "CANCELLED",
                remarks,
                currentUser.getUserId()
        );

        log.info("Invoice cancelled successfully. invoiceId={} userId={}",
                invoiceId,
                currentUser.getUserId());

        return Map.of(
                "invoiceId", invoiceId,
                "cancelled", true
        );
    }

    private void validatePatientAndAppointment(
            CurrentUser currentUser,
            Long patientId,
            Long appointmentId
    ) {

        validator.validatePatientId(patientId);
        validator.validateAppointmentId(appointmentId);

        boolean patientExists = repository.patientExists(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                patientId
        );

        validator.validatePatientExists(
                patientExists,
                patientId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        boolean appointmentExists = repository.appointmentExistsForPatient(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                patientId,
                appointmentId
        );

        validator.validateAppointmentForPatient(
                appointmentExists,
                appointmentId,
                patientId
        );

        log.info("Patient and appointment validated. patientId={} appointmentId={}",
                patientId,
                appointmentId);
    }

    private List<InvoiceItemCalculated> calculateItems(
            CurrentUser currentUser,
            List<InvoiceItemRequest> requestItems
    ) {

        if (requestItems == null || requestItems.isEmpty()) {
            throw ApiException.badRequest("At least one billing item is required.");
        }

        List<InvoiceItemCalculated> calculatedItems = new ArrayList<>();

        for (InvoiceItemRequest item : requestItems) {

            validator.validateInvoiceItemRequest(item);

            BillingServiceRecord service = repository.findService(
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    currentUser.getBranchId(),
                    item.serviceType(),
                    item.referenceId()
            ).orElse(null);

            validator.validateBillingServiceFound(
                    service,
                    item.serviceType(),
                    item.referenceId()
            );

            BigDecimal quantity = normalizeAmount(item.quantity(), BigDecimal.ONE);
            BigDecimal unitPrice = service.amount() == null ? BigDecimal.ZERO : service.amount();
            BigDecimal discountAmount = normalizeAmount(item.discountAmount(), BigDecimal.ZERO);
            BigDecimal taxRate = service.taxRate() == null ? BigDecimal.ZERO : service.taxRate();

            BigDecimal grossLineAmount = quantity
                    .multiply(unitPrice)
                    .setScale(2, RoundingMode.HALF_UP);

            validator.validateDiscountWithinLineAmount(
                    discountAmount,
                    grossLineAmount
            );

            BigDecimal taxableAmount = grossLineAmount
                    .subtract(discountAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal taxAmount = taxableAmount
                    .multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal lineTotal = taxableAmount
                    .add(taxAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            log.info("Billing service resolved. serviceType={} referenceId={} amount={} lineTotal={}",
                    service.serviceType(),
                    service.referenceId(),
                    unitPrice,
                    lineTotal);

            calculatedItems.add(new InvoiceItemCalculated(
                    service.serviceType(),
                    service.referenceId(),
                    service.serviceCode(),
                    service.serviceName(),
                    service.category(),
                    quantity,
                    unitPrice,
                    discountAmount,
                    taxableAmount,
                    taxRate,
                    taxAmount,
                    lineTotal
            ));
        }

        validator.validateCalculatedItems(calculatedItems);

        return calculatedItems;
    }

    private InvoiceTotals calculateTotals(List<InvoiceItemCalculated> items) {

        BigDecimal grossAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal taxableAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;

        for (InvoiceItemCalculated item : items) {
            grossAmount = grossAmount.add(item.quantity().multiply(item.unitPrice()));
            discountAmount = discountAmount.add(item.discountAmount());
            taxableAmount = taxableAmount.add(item.taxableAmount());
            taxAmount = taxAmount.add(item.taxAmount());
            netAmount = netAmount.add(item.lineTotal());
        }

        InvoiceTotals totals = new InvoiceTotals(
                grossAmount.setScale(2, RoundingMode.HALF_UP),
                discountAmount.setScale(2, RoundingMode.HALF_UP),
                taxableAmount.setScale(2, RoundingMode.HALF_UP),
                taxAmount.setScale(2, RoundingMode.HALF_UP),
                netAmount.setScale(2, RoundingMode.HALF_UP)
        );

        validator.validateInvoiceTotals(totals);

        log.info("Invoice total calculated. gross={} discount={} taxable={} tax={} net={}",
                totals.grossAmount(),
                totals.discountAmount(),
                totals.taxableAmount(),
                totals.taxAmount(),
                totals.netAmount());

        return totals;
    }

    private String resolvePaymentStatus(
            BigDecimal netAmount,
            BigDecimal paidAmount
    ) {

        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "UNPAID";
        }

        if (paidAmount.compareTo(netAmount) >= 0) {
            return "PAID";
        }

        return "PARTIAL";
    }

    private BigDecimal normalizeAmount(
            BigDecimal value,
            BigDecimal defaultValue
    ) {
        if (value == null) {
            return defaultValue;
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String emptyDefault(
            String value,
            String defaultValue
    ) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private CurrentUser requireUser() {

        CurrentUser currentUser = TenantContext.get();

        if (currentUser == null) {
            throw ApiException.unauthorized("Authentication required.");
        }

        return currentUser;
    }

    private void requirePermission(
            CurrentUser currentUser,
            String permission
    ) {

        if (!currentUser.hasPermission(permission)) {
            log.warn("Permission denied. userId={} tenantId={} hospitalId={} permission={}",
                    currentUser.getUserId(),
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    permission);

            throw ApiException.forbidden("Permission denied.");
        }
    }

    public record InvoiceRequest(
            @NotNull(message = "Patient id is required.")
            Long patientId,

            Long appointmentId,

            @NotEmpty(message = "Items are required.")
            List<InvoiceItemRequest> items,

            String notes
    ) {
    }

    public record InvoiceItemRequest(
            @NotNull(message = "Service type is required.")
            String serviceType,

            @NotNull(message = "Reference id is required.")
            Long referenceId,

            @DecimalMin(value = "0.01", message = "Quantity must be greater than zero.")
            BigDecimal quantity,

            @DecimalMin(value = "0.0", message = "Discount cannot be negative.")
            BigDecimal discountAmount
    ) {
    }

    public record PaymentRequest(
            @NotNull(message = "Payment amount is required.")
            @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero.")
            BigDecimal amount,

            String paymentMethod,
            String transactionReference,
            String remarks
    ) {
    }

    public record CancelRequest(
            String remarks
    ) {
    }

    public record BillingPreviewResponse(
            Long patientId,
            Long appointmentId,
            List<InvoiceItemCalculated> items,
            InvoiceTotals totals,
            String notes
    ) {
    }

    public record InvoiceDetailResponse(
            InvoiceRecord invoice,
            List<InvoiceItemRecord> items
    ) {
    }
}