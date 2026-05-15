package com.plasmit.diagnostic.report.alert.service;

import com.plasmit.diagnostic.report.alert.dto.request.*;
import com.plasmit.diagnostic.report.alert.dto.response.*;
import com.plasmit.diagnostic.report.alert.repository.CriticalAlertRepository;
import com.plasmit.diagnostic.report.alert.validator.CriticalAlertValidator;
import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.common.response.PageResponse;
import com.plasmit.diagnostic.report.common.response.PaginationMeta;
import com.plasmit.diagnostic.report.context.TenantContext;
import com.plasmit.diagnostic.report.reports.dto.response.DiagnosticReportResponse;
import com.plasmit.diagnostic.report.validator.CommonRequestValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CriticalAlertService {

    private final CriticalAlertRepository repository;
    private final CriticalAlertValidator validator;
    private final CommonRequestValidator commonRequestValidator;

    public CriticalAlertService(CriticalAlertRepository repository,
                                CriticalAlertValidator validator,
                                CommonRequestValidator commonRequestValidator) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
    }

    public PageResponse<CriticalAlertResponse> getAlerts(LocalDate fromDate,
                                                         LocalDate toDate,
                                                         String status,
                                                         String severity,
                                                         String search,
                                                         Integer page,
                                                         Integer limit) {

        validateContext();
        validator.validateList(fromDate, toDate, page, limit, status, severity);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        Long total = repository.countAlerts(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                severity,
                search
        );

        List<CriticalAlertResponse> rows = repository.findAlerts(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                severity,
                search,
                page,
                limit
        );

        return new PageResponse<>(
                rows,
                new PaginationMeta(page, limit, total)
        );
    }

    public CriticalAlertResponse getAlert(Long alertId) {
        validateContext();
        validator.validateAlertId(alertId);

        return repository.findAlertById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                alertId
        );
    }

    @Transactional
    public CriticalAlertResponse createAlert(Long reportId,
                                             CreateCriticalAlertRequest request) {

        validateContext();
        validator.validateReportId(reportId);
        validator.validateCreate(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        DiagnosticReportResponse report = repository.getReport(tenantId, hospitalId, branchId, reportId);

        if (!Boolean.TRUE.equals(report.criticalFlag())) {
            throw ApiException.workflow("Critical alert can be created only for critical reports.");
        }

        if (!"Released".equals(report.reportStatus())
                && !"Signed".equals(report.reportStatus())) {
            throw ApiException.workflow("Critical alert can be created only after report is signed or released.");
        }

        String alertNo = "CRT-" + System.currentTimeMillis();

        CriticalAlertResponse response = repository.createAlert(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                userId,
                report,
                request,
                alertNo
        );

        repository.insertEvent(
                tenantId,
                hospitalId,
                branchId,
                response.alertId(),
                reportId,
                null,
                "Open",
                "CRITICAL_ALERT_CREATED",
                null,
                request.alertMessage(),
                userId,
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    @Transactional
    public CriticalAlertAcknowledgementResponse acknowledge(Long alertId,
                                                           AcknowledgeCriticalAlertRequest request) {

        validateContext();
        validator.validateAlertId(alertId);
        validator.validateAcknowledge(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        CriticalAlertResponse alert = repository.findAlertById(tenantId, hospitalId, branchId, alertId);

        if ("Closed".equals(alert.alertStatus())) {
            throw ApiException.workflow("Closed alert cannot be acknowledged.");
        }

        if ("Acknowledged".equals(alert.alertStatus())) {
            throw ApiException.workflow("Alert is already acknowledged.");
        }

        CriticalAlertAcknowledgementResponse response = repository.acknowledge(
                tenantId,
                hospitalId,
                branchId,
                alert,
                userId,
                request
        );

        repository.insertEvent(
                tenantId,
                hospitalId,
                branchId,
                alertId,
                alert.reportId(),
                alert.alertStatus(),
                "Acknowledged",
                "CRITICAL_ALERT_ACKNOWLEDGED",
                null,
                request.acknowledgementNotes(),
                userId,
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    @Transactional
    public CriticalAlertEscalationResponse escalate(Long alertId,
                                                    EscalateCriticalAlertRequest request) {

        validateContext();
        validator.validateAlertId(alertId);
        validator.validateEscalate(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        CriticalAlertResponse alert = repository.findAlertById(tenantId, hospitalId, branchId, alertId);

        if ("Closed".equals(alert.alertStatus())) {
            throw ApiException.workflow("Closed alert cannot be escalated.");
        }

        if ("Acknowledged".equals(alert.alertStatus())) {
            throw ApiException.workflow("Acknowledged alert does not require escalation.");
        }

        Integer nextLevel = repository.nextEscalationLevel(tenantId, hospitalId, branchId, alertId);

        CriticalAlertEscalationResponse response = repository.escalate(
                tenantId,
                hospitalId,
                branchId,
                alert,
                userId,
                request,
                nextLevel
        );

        repository.insertEvent(
                tenantId,
                hospitalId,
                branchId,
                alertId,
                alert.reportId(),
                alert.alertStatus(),
                "Escalated",
                "CRITICAL_ALERT_ESCALATED",
                request.escalationReason(),
                "Escalated to " + request.escalatedToName(),
                userId,
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    @Transactional
    public CriticalAlertResponse close(Long alertId,
                                       CloseCriticalAlertRequest request) {

        validateContext();
        validator.validateAlertId(alertId);
        validator.validateClose(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        CriticalAlertResponse alert = repository.findAlertById(tenantId, hospitalId, branchId, alertId);

        if ("Closed".equals(alert.alertStatus())) {
            throw ApiException.workflow("Alert is already closed.");
        }

        if (!"Acknowledged".equals(alert.alertStatus())) {
            throw ApiException.workflow("Only acknowledged alert can be closed.");
        }

        CriticalAlertResponse response = repository.closeAlert(
                tenantId,
                hospitalId,
                branchId,
                alert,
                userId,
                request
        );

        repository.insertEvent(
                tenantId,
                hospitalId,
                branchId,
                alertId,
                alert.reportId(),
                alert.alertStatus(),
                "Closed",
                "CRITICAL_ALERT_CLOSED",
                null,
                request.closeNotes(),
                userId,
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    public List<CriticalAlertEventResponse> timeline(Long alertId) {
        validateContext();
        validator.validateAlertId(alertId);

        CriticalAlertResponse alert = repository.findAlertById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                alertId
        );

        return repository.findTimeline(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                alert.alertId()
        );
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }
}