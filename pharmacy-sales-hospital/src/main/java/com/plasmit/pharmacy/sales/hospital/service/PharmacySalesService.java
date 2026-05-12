package com.plasmit.pharmacy.sales.hospital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.plasmit.pharmacy.sales.hospital.exception.ApiException;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.PrescriptionMedicineRecord;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.PrescriptionRelationRecord;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.SaleItemCalculated;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.SaleItemRecord;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.SaleRecord;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.SaleTotals;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.StockBatchRecord;
import com.plasmit.pharmacy.sales.hospital.security.SecurityConfig.CurrentUser;
import com.plasmit.pharmacy.sales.hospital.security.SecurityConfig.TenantContext;
import com.plasmit.pharmacy.sales.hospital.validator.PharmacySalesValidator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PharmacySalesService {

    private static final Logger log = LoggerFactory.getLogger(PharmacySalesService.class);

    private final PharmacySalesRepository repository;
    private final PharmacySalesValidator validator;

    public PharmacySalesService(
            PharmacySalesRepository repository,
            PharmacySalesValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<SaleRecord> listSales(
            Long patientId,
            Long prescriptionId,
            String paymentStatus,
            String saleStatus,
            Integer page,
            Integer size
    ) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateOptionalPatientId(patientId);
        validator.validateOptionalPrescriptionId(prescriptionId);
        validator.validatePaymentStatusFilter(paymentStatus);
        validator.validateSaleStatusFilter(saleStatus);

        requirePermission(currentUser, "pharmacy_sales.view");

        int safePage = validator.validatePage(page);
        int safeSize = validator.validateSize(size);
        int offset = (safePage - 1) * safeSize;

        log.info("List pharmacy sales request. tenantId={} hospitalId={} patientId={} prescriptionId={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                patientId,
                prescriptionId);

        List<SaleRecord> sales = repository.findSales(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                patientId,
                prescriptionId,
                paymentStatus,
                saleStatus,
                safeSize,
                offset
        );

        validator.validateSaleList(
                sales,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        return sales;
    }

    public SaleDetailResponse getSale(Long saleId) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateSaleId(saleId);

        requirePermission(currentUser, "pharmacy_sales.view");

        SaleRecord sale = repository.findSaleById(
                saleId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateSaleFound(
                sale,
                saleId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<SaleItemRecord> items = repository.findSaleItems(
                saleId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateSaleItemsFetched(items, saleId);

        return new SaleDetailResponse(sale, items);
    }

    @Transactional
    public SaleDetailResponse createSale(CreateSaleRequest request) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateSaleRequest(request);

        requirePermission(currentUser, "pharmacy_sales.create");

        log.info("Create pharmacy sale request. tenantId={} hospitalId={} patientId={} itemCount={}",
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.patientId(),
                request.items() == null ? 0 : request.items().size());

        boolean patientExists = repository.patientExists(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.patientId()
        );

        validator.validatePatientExists(
                patientExists,
                request.patientId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        List<SaleItemCalculated> items = calculateItems(
                currentUser,
                request.items()
        );

        validator.validateCalculatedItems(items);

        SaleTotals totals = calculateTotals(items);

        validator.validateSaleTotals(totals);

        String saleNumber = repository.generateNextSaleNumber(currentUser.getHospitalId());

        Long saleId = repository.createSale(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getDepartmentId(),
                currentUser.getUserId(),
                saleNumber,
                request.patientId(),
                request.doctorId(),
                request.appointmentId(),
                request.prescriptionId(),
                totals,
                request.notes()
        );

        saveItemsAndReduceStock(currentUser, saleId, items);

        repository.saveStatusHistory(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                saleId,
                null,
                "ACTIVE",
                "Pharmacy sale created.",
                currentUser.getUserId()
        );

        log.info("Pharmacy sale created successfully. saleId={} saleNumber={} netAmount={}",
                saleId,
                saleNumber,
                totals.netAmount());

        return getSale(saleId);
    }

    @Transactional
    public SaleDetailResponse createSaleFromPrescription(CreateSaleFromPrescriptionRequest request) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateCreateFromPrescriptionRequest(request);

        requirePermission(currentUser, "pharmacy_sales.create");

        PrescriptionRelationRecord prescription = repository.findPrescriptionRelation(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                request.prescriptionId()
        ).orElse(null);

        validator.validatePrescriptionFound(
                prescription,
                request.prescriptionId()
        );

        List<PrescriptionMedicineRecord> prescriptionMedicines = repository.findPrescriptionMedicines(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                request.prescriptionId()
        );

        validator.validatePrescriptionMedicines(
                prescriptionMedicines,
                request.prescriptionId()
        );

        List<SaleItemRequest> saleItems = new ArrayList<>();

        for (PrescriptionMedicineRecord prescriptionMedicine : prescriptionMedicines) {
            StockBatchRecord batch = repository.findAvailableBatchByMedicineName(
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    currentUser.getBranchId(),
                    prescriptionMedicine.medicineName()
            ).orElse(null);

            validator.validateAvailableBatchFound(
                    batch,
                    prescriptionMedicine.medicineName()
            );

            saleItems.add(new SaleItemRequest(
                    batch.batchId(),
                    BigDecimal.ONE,
                    BigDecimal.ZERO
            ));
        }

        CreateSaleRequest saleRequest = new CreateSaleRequest(
                prescription.patientId(),
                prescription.doctorId(),
                prescription.appointmentId(),
                prescription.prescriptionId(),
                saleItems,
                request.notes() == null ? "Sale generated from prescription." : request.notes()
        );

        log.info("Creating pharmacy sale from prescription. prescriptionId={} medicineCount={}",
                request.prescriptionId(),
                saleItems.size());

        return createSale(saleRequest);
    }

    @Transactional
    public SaleDetailResponse addPayment(Long saleId, PaymentRequest request) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateSaleId(saleId);
        validator.validatePaymentRequest(request);

        requirePermission(currentUser, "pharmacy_sales.payment");

        SaleRecord sale = repository.findSaleById(
                saleId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateSaleFound(
                sale,
                saleId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateSaleCanAcceptPayment(sale);

        validator.validatePaymentAmountWithinBalance(
                request.amount(),
                sale.balanceAmount()
        );

        String paymentNumber = repository.generateNextPaymentNumber(currentUser.getHospitalId());

        Long paymentId = repository.createPayment(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                saleId,
                sale.patientId(),
                paymentNumber,
                request,
                currentUser.getUserId()
        );

        BigDecimal newPaidAmount = sale.paidAmount()
                .add(request.amount())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal newBalanceAmount = sale.netAmount()
                .subtract(newPaidAmount)
                .setScale(2, RoundingMode.HALF_UP);

        if (newBalanceAmount.compareTo(BigDecimal.ZERO) < 0) {
            newBalanceAmount = BigDecimal.ZERO;
        }

        String newPaymentStatus = resolvePaymentStatus(
                sale.netAmount(),
                newPaidAmount
        );

        repository.updateSalePaymentTotals(
                saleId,
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
                saleId,
                sale.paymentStatus(),
                newPaymentStatus,
                "Payment received: " + request.amount(),
                currentUser.getUserId()
        );

        log.info("Pharmacy payment added. saleId={} paymentId={} amount={} status={}",
                saleId,
                paymentId,
                request.amount(),
                newPaymentStatus);

        return getSale(saleId);
    }

    @Transactional
    public Map<String, Object> cancelSale(Long saleId, CancelRequest request) {
        CurrentUser currentUser = requireUser();

        validator.validateTenantHospitalContext(
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateBranchContext(currentUser.getBranchId());
        validator.validateSaleId(saleId);

        requirePermission(currentUser, "pharmacy_sales.cancel");

        SaleRecord sale = repository.findSaleById(
                saleId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId()
        ).orElse(null);

        validator.validateSaleFound(
                sale,
                saleId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        validator.validateSaleCanCancel(sale);

        String remarks = request == null || request.remarks() == null || request.remarks().isBlank()
                ? "Pharmacy sale cancelled."
                : request.remarks();

        int updated = repository.cancelSale(
                saleId,
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                remarks
        );

        validator.validateCancelCount(
                updated,
                saleId,
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        );

        repository.saveStatusHistory(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                saleId,
                sale.saleStatus(),
                "CANCELLED",
                remarks,
                currentUser.getUserId()
        );

        log.info("Pharmacy sale cancelled. saleId={} userId={}",
                saleId,
                currentUser.getUserId());

        return Map.of(
                "saleId", saleId,
                "cancelled", true
        );
    }

    private List<SaleItemCalculated> calculateItems(
            CurrentUser currentUser,
            List<SaleItemRequest> requestItems
    ) {

        validator.validateSaleItems(requestItems);

        List<SaleItemCalculated> calculatedItems = new ArrayList<>();

        for (SaleItemRequest item : requestItems) {

            validator.validateSaleItem(item);

            StockBatchRecord batch = repository.findStockBatch(
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    currentUser.getBranchId(),
                    item.batchId()
            ).orElse(null);

            validator.validateStockBatchFound(
                    batch,
                    item.batchId()
            );

            BigDecimal quantity = normalize(
                    item.quantity(),
                    BigDecimal.ONE
            );

            validator.validateSufficientStock(
                    batch,
                    quantity
            );

            BigDecimal unitPrice = batch.salePrice() == null ? BigDecimal.ZERO : batch.salePrice();
            BigDecimal mrp = batch.mrp() == null ? BigDecimal.ZERO : batch.mrp();
            BigDecimal discountAmount = normalize(item.discountAmount(), BigDecimal.ZERO);
            BigDecimal taxRate = batch.taxRate() == null ? BigDecimal.ZERO : batch.taxRate();

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

            BigDecimal afterStock = batch.currentQuantity()
                    .subtract(quantity)
                    .setScale(2, RoundingMode.HALF_UP);

            calculatedItems.add(new SaleItemCalculated(
                    batch.medicineId(),
                    batch.batchId(),
                    batch.medicineCode(),
                    batch.medicineName(),
                    batch.batchNumber(),
                    quantity,
                    unitPrice,
                    mrp,
                    discountAmount,
                    taxableAmount,
                    taxRate,
                    taxAmount,
                    lineTotal,
                    batch.currentQuantity(),
                    afterStock
            ));

            log.info("Sale item calculated. medicineId={} batchId={} quantity={} lineTotal={} beforeStock={} afterStock={}",
                    batch.medicineId(),
                    batch.batchId(),
                    quantity,
                    lineTotal,
                    batch.currentQuantity(),
                    afterStock);
        }

        validator.validateCalculatedItems(calculatedItems);

        return calculatedItems;
    }

    private SaleTotals calculateTotals(List<SaleItemCalculated> items) {

        validator.validateCalculatedItems(items);

        BigDecimal grossAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal taxableAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;

        for (SaleItemCalculated item : items) {
            grossAmount = grossAmount.add(item.quantity().multiply(item.unitPrice()));
            discountAmount = discountAmount.add(item.discountAmount());
            taxableAmount = taxableAmount.add(item.taxableAmount());
            taxAmount = taxAmount.add(item.taxAmount());
            netAmount = netAmount.add(item.lineTotal());
        }

        SaleTotals totals = new SaleTotals(
                grossAmount.setScale(2, RoundingMode.HALF_UP),
                discountAmount.setScale(2, RoundingMode.HALF_UP),
                taxableAmount.setScale(2, RoundingMode.HALF_UP),
                taxAmount.setScale(2, RoundingMode.HALF_UP),
                netAmount.setScale(2, RoundingMode.HALF_UP)
        );

        validator.validateSaleTotals(totals);

        log.info("Pharmacy sale total calculated. gross={} discount={} tax={} net={}",
                totals.grossAmount(),
                totals.discountAmount(),
                totals.taxAmount(),
                totals.netAmount());

        return totals;
    }

    private void saveItemsAndReduceStock(
            CurrentUser currentUser,
            Long saleId,
            List<SaleItemCalculated> items
    ) {

        validator.validateCalculatedItems(items);

        for (SaleItemCalculated item : items) {
            repository.createSaleItem(
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    currentUser.getBranchId(),
                    saleId,
                    item
            );

            repository.reduceBatchStock(
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    currentUser.getBranchId(),
                    item.batchId(),
                    item.afterStock(),
                    currentUser.getUserId()
            );

            repository.createStockMovement(
                    currentUser.getTenantId(),
                    currentUser.getHospitalId(),
                    currentUser.getBranchId(),
                    item.medicineId(),
                    item.batchId(),
                    saleId,
                    item.quantity(),
                    item.beforeStock(),
                    item.afterStock(),
                    currentUser.getUserId()
            );
        }
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

    private BigDecimal normalize(
            BigDecimal value,
            BigDecimal defaultValue
    ) {
        return value == null ? defaultValue : value.setScale(2, RoundingMode.HALF_UP);
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

    public record CreateSaleRequest(
            @NotNull(message = "Patient id is required.")
            Long patientId,

            Long doctorId,
            Long appointmentId,
            Long prescriptionId,

            @NotEmpty(message = "Sale items are required.")
            List<@Valid SaleItemRequest> items,

            String notes
    ) {
    }

    public record SaleItemRequest(
            @NotNull(message = "Batch id is required.")
            Long batchId,

            @NotNull(message = "Quantity is required.")
            @DecimalMin(value = "0.01", message = "Quantity must be greater than zero.")
            BigDecimal quantity,

            @DecimalMin(value = "0.0", message = "Discount cannot be negative.")
            BigDecimal discountAmount
    ) {
    }

    public record CreateSaleFromPrescriptionRequest(
            @NotNull(message = "Prescription id is required.")
            Long prescriptionId,

            String notes
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

    public record SaleDetailResponse(
            SaleRecord sale,
            List<SaleItemRecord> items
    ) {
    }
}