package com.plasmit.pharmacy.sales.hospital.validator;

import java.math.BigDecimal;
import java.util.List;

import com.plasmit.pharmacy.sales.hospital.exception.ApiException;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.PrescriptionMedicineRecord;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.PrescriptionRelationRecord;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.SaleItemCalculated;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.SaleItemRecord;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.SaleRecord;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.SaleTotals;
import com.plasmit.pharmacy.sales.hospital.repository.PharmacySalesRepository.StockBatchRecord;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.CreateSaleFromPrescriptionRequest;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.CreateSaleRequest;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.PaymentRequest;
import com.plasmit.pharmacy.sales.hospital.service.PharmacySalesService.SaleItemRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PharmacySalesValidator {

    private static final Logger log = LoggerFactory.getLogger(PharmacySalesValidator.class);

    public void validateTenantHospitalContext(Long tenantId, Long hospitalId) {
        if (tenantId == null || hospitalId == null) {
            log.warn("Pharmacy sales validation failed. reason=TENANT_OR_HOSPITAL_MISSING tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }

        if (tenantId <= 0 || hospitalId <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_TENANT_OR_HOSPITAL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.unauthorized("Invalid tenant or hospital context.");
        }
    }

    public void validateBranchContext(Long branchId) {
        if (branchId != null && branchId <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_BRANCH_ID branchId={}", branchId);
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
            log.warn("Pharmacy sales validation warning. reason=PAGE_SIZE_LIMIT_EXCEEDED requestedSize={}", size);
            return 100;
        }

        return size;
    }

    public void validateSaleId(Long saleId) {
        if (saleId == null || saleId <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_SALE_ID saleId={}", saleId);
            throw ApiException.badRequest("Invalid pharmacy sale id.");
        }
    }

    public void validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_PATIENT_ID patientId={}", patientId);
            throw ApiException.badRequest("Invalid patient id.");
        }
    }

    public void validateOptionalPatientId(Long patientId) {
        if (patientId != null && patientId <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_PATIENT_ID patientId={}", patientId);
            throw ApiException.badRequest("Invalid patient id.");
        }
    }

    public void validatePrescriptionId(Long prescriptionId) {
        if (prescriptionId == null || prescriptionId <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_PRESCRIPTION_ID prescriptionId={}", prescriptionId);
            throw ApiException.badRequest("Invalid prescription id.");
        }
    }

    public void validateOptionalPrescriptionId(Long prescriptionId) {
        if (prescriptionId != null && prescriptionId <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_PRESCRIPTION_ID prescriptionId={}", prescriptionId);
            throw ApiException.badRequest("Invalid prescription id.");
        }
    }

    public void validateBatchId(Long batchId) {
        if (batchId == null || batchId <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_BATCH_ID batchId={}", batchId);
            throw ApiException.badRequest("Invalid stock batch id.");
        }
    }

    public void validatePaymentStatusFilter(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return;
        }

        if (!paymentStatus.equalsIgnoreCase("UNPAID")
                && !paymentStatus.equalsIgnoreCase("PARTIAL")
                && !paymentStatus.equalsIgnoreCase("PAID")) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_PAYMENT_STATUS paymentStatus={}", paymentStatus);
            throw ApiException.badRequest("Payment status must be UNPAID, PARTIAL or PAID.");
        }
    }

    public void validateSaleStatusFilter(String saleStatus) {
        if (saleStatus == null || saleStatus.isBlank()) {
            return;
        }

        if (!saleStatus.equalsIgnoreCase("ACTIVE")
                && !saleStatus.equalsIgnoreCase("CANCELLED")) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_SALE_STATUS saleStatus={}", saleStatus);
            throw ApiException.badRequest("Sale status must be ACTIVE or CANCELLED.");
        }
    }

    public void validateCreateSaleRequest(CreateSaleRequest request) {
        if (request == null) {
            log.warn("Pharmacy sales validation failed. reason=CREATE_SALE_REQUEST_NULL");
            throw ApiException.badRequest("Sale request is required.");
        }

        validatePatientId(request.patientId());

        if (request.doctorId() != null && request.doctorId() <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_DOCTOR_ID doctorId={}", request.doctorId());
            throw ApiException.badRequest("Invalid doctor id.");
        }

        if (request.appointmentId() != null && request.appointmentId() <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_APPOINTMENT_ID appointmentId={}", request.appointmentId());
            throw ApiException.badRequest("Invalid appointment id.");
        }

        if (request.prescriptionId() != null && request.prescriptionId() <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_PRESCRIPTION_ID prescriptionId={}", request.prescriptionId());
            throw ApiException.badRequest("Invalid prescription id.");
        }

        validateSaleItems(request.items());
    }

    public void validateCreateFromPrescriptionRequest(CreateSaleFromPrescriptionRequest request) {
        if (request == null) {
            log.warn("Pharmacy sales validation failed. reason=CREATE_FROM_PRESCRIPTION_REQUEST_NULL");
            throw ApiException.badRequest("Prescription sale request is required.");
        }

        validatePrescriptionId(request.prescriptionId());
    }

    public void validateSaleItems(List<SaleItemRequest> items) {
        if (items == null || items.isEmpty()) {
            log.warn("Pharmacy sales validation failed. reason=SALE_ITEMS_REQUIRED");
            throw ApiException.badRequest("At least one sale item is required.");
        }

        for (SaleItemRequest item : items) {
            validateSaleItem(item);
        }
    }

    public void validateSaleItem(SaleItemRequest item) {
        if (item == null) {
            log.warn("Pharmacy sales validation failed. reason=SALE_ITEM_NULL");
            throw ApiException.badRequest("Sale item is required.");
        }

        validateBatchId(item.batchId());
        validatePositiveAmount(item.quantity(), "Quantity");
        validateNonNegativeAmount(item.discountAmount(), "Discount");
    }

    public void validatePatientExists(boolean exists, Long patientId, Long tenantId, Long hospitalId) {
        if (!exists) {
            log.warn("Pharmacy sales validation failed. reason=PATIENT_NOT_FOUND patientId={} tenantId={} hospitalId={}",
                    patientId, tenantId, hospitalId);
            throw ApiException.notFound("Patient not found.");
        }
    }

    public void validatePrescriptionFound(PrescriptionRelationRecord prescription, Long prescriptionId) {
        if (prescription == null) {
            log.warn("Pharmacy sales validation failed. reason=PRESCRIPTION_NOT_FOUND prescriptionId={}", prescriptionId);
            throw ApiException.notFound("Prescription not found.");
        }
    }

    public void validatePrescriptionMedicines(List<PrescriptionMedicineRecord> medicines, Long prescriptionId) {
        if (medicines == null || medicines.isEmpty()) {
            log.warn("Pharmacy sales validation failed. reason=PRESCRIPTION_MEDICINES_EMPTY prescriptionId={}", prescriptionId);
            throw ApiException.badRequest("No medicines found in prescription.");
        }
    }

    public void validateStockBatchFound(StockBatchRecord batch, Long batchId) {
        if (batch == null) {
            log.warn("Pharmacy sales validation failed. reason=STOCK_BATCH_NOT_FOUND batchId={}", batchId);
            throw ApiException.notFound("Stock batch not found.");
        }
    }

    public void validateAvailableBatchFound(StockBatchRecord batch, String medicineName) {
        if (batch == null) {
            log.warn("Pharmacy sales validation failed. reason=AVAILABLE_BATCH_NOT_FOUND medicineName={}", medicineName);
            throw ApiException.notFound("No available pharmacy stock found for medicine: " + medicineName);
        }
    }

    public void validateSufficientStock(StockBatchRecord batch, BigDecimal quantity) {
        if (batch.currentQuantity() == null || batch.currentQuantity().compareTo(quantity) < 0) {
            log.warn("Pharmacy sales validation failed. reason=INSUFFICIENT_STOCK batchId={} medicineName={} currentQuantity={} requestedQuantity={}",
                    batch.batchId(), batch.medicineName(), batch.currentQuantity(), quantity);
            throw ApiException.badRequest("Insufficient stock for medicine: " + batch.medicineName());
        }
    }

    public void validateDiscountWithinLineAmount(BigDecimal discountAmount, BigDecimal grossLineAmount) {
        BigDecimal safeDiscount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal safeGross = grossLineAmount == null ? BigDecimal.ZERO : grossLineAmount;

        if (safeDiscount.compareTo(safeGross) > 0) {
            log.warn("Pharmacy sales validation failed. reason=DISCOUNT_GREATER_THAN_LINE_AMOUNT discount={} gross={}",
                    safeDiscount, safeGross);
            throw ApiException.badRequest("Discount cannot be greater than line amount.");
        }
    }

    public void validateCalculatedItems(List<SaleItemCalculated> items) {
        if (items == null || items.isEmpty()) {
            log.warn("Pharmacy sales validation failed. reason=CALCULATED_ITEMS_EMPTY");
            throw ApiException.badRequest("At least one sale item is required.");
        }
    }

    public void validateSaleTotals(SaleTotals totals) {
        if (totals == null || totals.netAmount() == null || totals.netAmount().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_SALE_TOTALS totals={}", totals);
            throw ApiException.badRequest("Invalid pharmacy sale totals.");
        }
    }

    public void validateSaleFound(SaleRecord sale, Long saleId, Long tenantId, Long hospitalId) {
        if (sale == null) {
            log.warn("Pharmacy sales validation failed. reason=SALE_NOT_FOUND saleId={} tenantId={} hospitalId={}",
                    saleId, tenantId, hospitalId);
            throw ApiException.notFound("Pharmacy sale not found.");
        }
    }

    public void validateSaleItemsFetched(List<SaleItemRecord> items, Long saleId) {
        if (items == null) {
            log.warn("Pharmacy sales validation failed. reason=SALE_ITEM_LIST_NULL saleId={}", saleId);
            throw ApiException.badRequest("Unable to fetch pharmacy sale items.");
        }
    }

    public void validateSaleList(List<SaleRecord> sales, Long tenantId, Long hospitalId) {
        if (sales == null) {
            log.warn("Pharmacy sales validation failed. reason=SALE_LIST_NULL tenantId={} hospitalId={}",
                    tenantId, hospitalId);
            throw ApiException.badRequest("Unable to fetch pharmacy sales.");
        }
    }

    public void validatePaymentRequest(PaymentRequest request) {
        if (request == null) {
            log.warn("Pharmacy sales validation failed. reason=PAYMENT_REQUEST_NULL");
            throw ApiException.badRequest("Payment request is required.");
        }

        validatePositiveAmount(request.amount(), "Payment amount");
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
            log.warn("Pharmacy sales validation failed. reason=INVALID_PAYMENT_METHOD paymentMethod={}", paymentMethod);
            throw ApiException.badRequest("Invalid payment method.");
        }
    }

    public void validateSaleCanAcceptPayment(SaleRecord sale) {
        if ("CANCELLED".equalsIgnoreCase(sale.saleStatus())) {
            log.warn("Pharmacy sales validation failed. reason=PAYMENT_ON_CANCELLED_SALE saleId={}", sale.id());
            throw ApiException.badRequest("Payment cannot be added to cancelled sale.");
        }

        if (sale.balanceAmount() == null || sale.balanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Pharmacy sales validation failed. reason=NO_BALANCE_DUE saleId={} balance={}",
                    sale.id(), sale.balanceAmount());
            throw ApiException.badRequest("Sale has no pending balance.");
        }
    }

    public void validatePaymentAmountWithinBalance(BigDecimal paymentAmount, BigDecimal balanceAmount) {
        if (paymentAmount.compareTo(balanceAmount) > 0) {
            log.warn("Pharmacy sales validation failed. reason=PAYMENT_GREATER_THAN_BALANCE paymentAmount={} balanceAmount={}",
                    paymentAmount, balanceAmount);
            throw ApiException.badRequest("Payment amount cannot be greater than sale balance.");
        }
    }

    public void validateSaleCanCancel(SaleRecord sale) {
        if ("CANCELLED".equalsIgnoreCase(sale.saleStatus())) {
            log.warn("Pharmacy sales validation failed. reason=SALE_ALREADY_CANCELLED saleId={}", sale.id());
            throw ApiException.badRequest("Sale is already cancelled.");
        }
    }

    public void validateCancelCount(int updated, Long saleId, Long tenantId, Long hospitalId) {
        if (updated == 0) {
            log.warn("Pharmacy sales validation failed. reason=SALE_CANCEL_FAILED saleId={} tenantId={} hospitalId={}",
                    saleId, tenantId, hospitalId);
            throw ApiException.notFound("Pharmacy sale not found.");
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Pharmacy sales validation failed. reason=INVALID_AMOUNT field={} amount={}", fieldName, amount);
            throw ApiException.badRequest(fieldName + " must be greater than zero.");
        }
    }

    private void validateNonNegativeAmount(BigDecimal amount, String fieldName) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Pharmacy sales validation failed. reason=NEGATIVE_AMOUNT field={} amount={}", fieldName, amount);
            throw ApiException.badRequest(fieldName + " cannot be negative.");
        }
    }
}