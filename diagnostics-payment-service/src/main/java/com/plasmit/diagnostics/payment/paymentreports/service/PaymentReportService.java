package com.plasmit.diagnostics.payment.paymentreports.service;

import com.plasmit.diagnostics.payment.context.TenantContext;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentBootstrapResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentSummaryResponse;
import com.plasmit.diagnostics.payment.paymentreports.repository.PaymentReportRepository;
import com.plasmit.diagnostics.payment.paymentreports.validator.PaymentReportValidator;
import com.plasmit.diagnostics.payment.validator.CommonRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.plasmit.diagnostics.payment.common.response.PageResponse;
import com.plasmit.diagnostics.payment.common.response.PaginationMeta;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentLedgerRowResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.CollectionMixResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.DepartmentBillingResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.DueAgingResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.SettlementExceptionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasmit.diagnostics.payment.common.exception.ApiException;
import com.plasmit.diagnostics.payment.paymentreports.dto.request.ReconciliationRunRequest;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.ReconciliationRunResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.request.ExportJobRequest;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.ExportJobResponse;
import com.plasmit.diagnostics.payment.paymentreports.dto.response.PaymentAuditResponse;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class PaymentReportService {
	
    private static final Logger log = LoggerFactory.getLogger(PaymentReportService.class);
    
    private final PaymentReportRepository paymentReportRepository;
    private final CommonRequestValidator commonRequestValidator;
    private final PaymentReportValidator paymentReportValidator;
    private final ObjectMapper objectMapper;
    

    public PaymentReportService(PaymentReportRepository paymentReportRepository,
            CommonRequestValidator commonRequestValidator,
            PaymentReportValidator paymentReportValidator,
            ObjectMapper objectMapper) {
this.paymentReportRepository = paymentReportRepository;
this.commonRequestValidator = commonRequestValidator;
this.paymentReportValidator = paymentReportValidator;
this.objectMapper = objectMapper;
}

    public PaymentSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate) {

        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();

        paymentReportValidator.validateSummaryRequest(fromDate, toDate);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        log.info("Fetching payment report summary. tenantId={} hospitalId={} branchId={} fromDate={} toDate={}",
                tenantId, hospitalId, branchId, fromDate, toDate);

        return paymentReportRepository.getSummary(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate
        );
    }

    public PaymentBootstrapResponse getBootstrap(LocalDate fromDate, LocalDate toDate) {

        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();

        paymentReportValidator.validateBootstrapRequest(fromDate, toDate);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();

        log.info("Fetching payment report bootstrap. tenantId={} hospitalId={} fromDate={} toDate={}",
                tenantId, hospitalId, fromDate, toDate);

        PaymentBootstrapResponse response = new PaymentBootstrapResponse();

        response.setBranches(paymentReportRepository.findBranches(tenantId, hospitalId, fromDate, toDate));
        response.setDepartments(paymentReportRepository.findDepartments(tenantId, hospitalId, fromDate, toDate));
        response.setPayerTypes(paymentReportRepository.findPayerTypes(tenantId, hospitalId, fromDate, toDate));
        response.setPaymentMethods(paymentReportRepository.findPaymentMethods(tenantId, hospitalId, fromDate, toDate));
        response.setStatuses(paymentReportRepository.findStatuses(tenantId, hospitalId, fromDate, toDate));
        response.setSettlementStatuses(paymentReportRepository.findSettlementStatuses(tenantId, hospitalId, fromDate, toDate));
        response.setCashiers(paymentReportRepository.findCashiers(tenantId, hospitalId, fromDate, toDate));

        return response;
    }
    
    public PageResponse<PaymentLedgerRowResponse> getLedger(LocalDate fromDate,
            LocalDate toDate,
            String department,
            String payerType,
            String status,
            String paymentMethod,
            String settlementStatus,
            Long cashierId,
            String search,
            Integer page,
            Integer limit,
            String sortBy,
            String sortOrder) {

commonRequestValidator.validateTenantContext();
commonRequestValidator.validateHospitalContext();
commonRequestValidator.validateUserContext();
commonRequestValidator.validateBranchRequired();

paymentReportValidator.validateLedgerRequest(fromDate, toDate, page, limit, sortBy, sortOrder);

Long tenantId = TenantContext.getTenantId();
Long hospitalId = TenantContext.getHospitalId();
String branchId = TenantContext.getBranchId();

log.info("Fetching payment ledger. tenantId={} hospitalId={} branchId={} fromDate={} toDate={} page={} limit={}",
tenantId, hospitalId, branchId, fromDate, toDate, page, limit);

Long total = paymentReportRepository.countLedgerRows(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
department,
payerType,
status,
paymentMethod,
settlementStatus,
cashierId,
search
);

List<PaymentLedgerRowResponse> rows = paymentReportRepository.findLedgerRows(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
department,
payerType,
status,
paymentMethod,
settlementStatus,
cashierId,
search,
page,
limit,
sortBy,
sortOrder
);

return new PageResponse<>(
rows,
new PaginationMeta(page, limit, total)
);
}
    public List<CollectionMixResponse> getCollectionMix(LocalDate fromDate,
            LocalDate toDate,
            String department,
            String payerType,
            String status,
            Long cashierId) {

commonRequestValidator.validateTenantContext();
commonRequestValidator.validateHospitalContext();
commonRequestValidator.validateUserContext();
commonRequestValidator.validateBranchRequired();

paymentReportValidator.validateSummaryRequest(fromDate, toDate);

Long tenantId = TenantContext.getTenantId();
Long hospitalId = TenantContext.getHospitalId();
String branchId = TenantContext.getBranchId();

log.info("Fetching collection mix. tenantId={} hospitalId={} branchId={} fromDate={} toDate={}",
tenantId, hospitalId, branchId, fromDate, toDate);

List<CollectionMixResponse> items = paymentReportRepository.findCollectionMix(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
department,
payerType,
status,
cashierId
);

long totalCollected = items.stream()
.mapToLong(item -> item.getCollectedPaise() == null ? 0L : item.getCollectedPaise())
.sum();

for (CollectionMixResponse item : items) {
if (totalCollected <= 0 || item.getCollectedPaise() == null) {
item.setPercentage(0.0);
} else {
BigDecimal percentage = BigDecimal.valueOf(item.getCollectedPaise())
.multiply(BigDecimal.valueOf(100))
.divide(BigDecimal.valueOf(totalCollected), 2, RoundingMode.HALF_UP);

item.setPercentage(percentage.doubleValue());
}
}

return items;
}
    public List<DepartmentBillingResponse> getDepartmentBilling(LocalDate fromDate,
            LocalDate toDate,
            String payerType,
            String status,
            String paymentMethod,
            Long cashierId) {

commonRequestValidator.validateTenantContext();
commonRequestValidator.validateHospitalContext();
commonRequestValidator.validateUserContext();
commonRequestValidator.validateBranchRequired();

paymentReportValidator.validateDepartmentBillingRequest(fromDate, toDate);

Long tenantId = TenantContext.getTenantId();
Long hospitalId = TenantContext.getHospitalId();
String branchId = TenantContext.getBranchId();

log.info("Fetching department billing. tenantId={} hospitalId={} branchId={} fromDate={} toDate={}",
tenantId, hospitalId, branchId, fromDate, toDate);

return paymentReportRepository.findDepartmentBilling(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
payerType,
status,
paymentMethod,
cashierId
);
}
    public List<DueAgingResponse> getDueAging(LocalDate fromDate,
            LocalDate toDate,
            String department,
            String payerType,
            String status,
            String paymentMethod,
            Long cashierId) {

commonRequestValidator.validateTenantContext();
commonRequestValidator.validateHospitalContext();
commonRequestValidator.validateUserContext();
commonRequestValidator.validateBranchRequired();

paymentReportValidator.validateDueAgingRequest(fromDate, toDate);

Long tenantId = TenantContext.getTenantId();
Long hospitalId = TenantContext.getHospitalId();
String branchId = TenantContext.getBranchId();

log.info("Fetching due aging. tenantId={} hospitalId={} branchId={} fromDate={} toDate={}",
tenantId, hospitalId, branchId, fromDate, toDate);

return paymentReportRepository.findDueAging(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
department,
payerType,
status,
paymentMethod,
cashierId
);
}
    public List<SettlementExceptionResponse> getSettlementExceptions(LocalDate fromDate,
            LocalDate toDate,
            String paymentMethod,
            String severity,
            String status,
            String owner) {

commonRequestValidator.validateTenantContext();
commonRequestValidator.validateHospitalContext();
commonRequestValidator.validateUserContext();
commonRequestValidator.validateBranchRequired();

paymentReportValidator.validateSettlementExceptionRequest(fromDate, toDate);

Long tenantId = TenantContext.getTenantId();
Long hospitalId = TenantContext.getHospitalId();
String branchId = TenantContext.getBranchId();

log.info("Fetching settlement exceptions. tenantId={} hospitalId={} branchId={} fromDate={} toDate={}",
tenantId, hospitalId, branchId, fromDate, toDate);

return paymentReportRepository.findSettlementExceptions(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
paymentMethod,
severity,
status,
owner
);
}
    @Transactional
    public ReconciliationRunResponse createReconciliationRun(ReconciliationRunRequest request,
                                                             String idempotencyKey) {

        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
        commonRequestValidator.validateIdempotencyKey(idempotencyKey);

        paymentReportValidator.validateReconciliationRunRequest(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        log.info("Creating reconciliation run. tenantId={} hospitalId={} branchId={} fromDate={} toDate={} channels={}",
                tenantId, hospitalId, branchId, request.getFromDate(), request.getToDate(), request.getChannels());

        String channelsJson = toJson(request.getChannels());

        return paymentReportRepository.createReconciliationRun(
                tenantId,
                hospitalId,
                branchId,
                userId,
                request,
                channelsJson
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw ApiException.validation("Invalid JSON payload.");
        }
    }   
    public ReconciliationRunResponse getReconciliationRun(Long runId) {

        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();

        if (runId == null || runId <= 0) {
            throw ApiException.validation("Valid runId is required.");
        }

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        log.info("Fetching reconciliation run. tenantId={} hospitalId={} branchId={} runId={}",
                tenantId, hospitalId, branchId, runId);

        return paymentReportRepository.findReconciliationRunById(
                tenantId,
                hospitalId,
                branchId,
                runId
        );
    }
    @Transactional
    public ExportJobResponse createExportJob(ExportJobRequest request,
                                             String idempotencyKey) {

        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
        commonRequestValidator.validateIdempotencyKey(idempotencyKey);

        paymentReportValidator.validateExportJobRequest(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        log.info("Creating export job. tenantId={} hospitalId={} branchId={} reportType={} format={} fromDate={} toDate={}",
                tenantId,
                hospitalId,
                branchId,
                request.getReportType(),
                request.getFormat(),
                request.getFromDate(),
                request.getToDate()
        );

        String filtersJson = toJson(
                request.getFilters() == null
                        ? java.util.Collections.emptyMap()
                        : request.getFilters()
        );

        return paymentReportRepository.createExportJob(
                tenantId,
                hospitalId,
                branchId,
                userId,
                request,
                filtersJson
        );
    }
    public ExportJobResponse getExportJob(Long exportId) {

        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();

        if (exportId == null || exportId <= 0) {
            throw ApiException.validation("Valid exportId is required.");
        }

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        log.info("Fetching export job. tenantId={} hospitalId={} branchId={} exportId={}",
                tenantId, hospitalId, branchId, exportId);

        return paymentReportRepository.findExportJobById(
                tenantId,
                hospitalId,
                branchId,
                exportId
        );
    }
    public PageResponse<PaymentAuditResponse> getAudit(LocalDate fromDate,
            LocalDate toDate,
            String entityType,
            String action,
            Long actorUserId,
            Integer page,
            Integer limit) {

commonRequestValidator.validateTenantContext();
commonRequestValidator.validateHospitalContext();
commonRequestValidator.validateUserContext();
commonRequestValidator.validateBranchRequired();

paymentReportValidator.validateAuditRequest(fromDate, toDate, page, limit);

Long tenantId = TenantContext.getTenantId();
Long hospitalId = TenantContext.getHospitalId();
String branchId = TenantContext.getBranchId();

log.info("Fetching payment report audit. tenantId={} hospitalId={} branchId={} fromDate={} toDate={} page={} limit={}",
tenantId, hospitalId, branchId, fromDate, toDate, page, limit);

Long total = paymentReportRepository.countPaymentAudit(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
entityType,
action,
actorUserId
);

List<PaymentAuditResponse> rows = paymentReportRepository.findPaymentAudit(
tenantId,
hospitalId,
branchId,
fromDate,
toDate,
entityType,
action,
actorUserId,
page,
limit
);

return new PageResponse<>(
rows,
new PaginationMeta(page, limit, total)
);
}
}