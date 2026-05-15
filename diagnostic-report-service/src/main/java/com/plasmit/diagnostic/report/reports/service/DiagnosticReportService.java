package com.plasmit.diagnostic.report.reports.service;

import com.plasmit.diagnostic.report.common.exception.ApiException;
import com.plasmit.diagnostic.report.common.response.PageResponse;
import com.plasmit.diagnostic.report.common.response.PaginationMeta;
import com.plasmit.diagnostic.report.context.TenantContext;
import com.plasmit.diagnostic.report.reports.dto.request.*;
import com.plasmit.diagnostic.report.reports.dto.response.*;
import com.plasmit.diagnostic.report.reports.repository.DiagnosticReportRepository;
import com.plasmit.diagnostic.report.reports.validator.DiagnosticReportValidator;
import com.plasmit.diagnostic.report.validator.CommonRequestValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DiagnosticReportService {

    private final DiagnosticReportRepository repository;
    private final DiagnosticReportValidator validator;
    private final CommonRequestValidator commonRequestValidator;

    public DiagnosticReportService(DiagnosticReportRepository repository,
                                   DiagnosticReportValidator validator,
                                   CommonRequestValidator commonRequestValidator) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
    }

    public PageResponse<DiagnosticReportResponse> getReports(LocalDate fromDate,
                                                             LocalDate toDate,
                                                             String status,
                                                             String sourceType,
                                                             String department,
                                                             String search,
                                                             Integer page,
                                                             Integer limit) {

        validateContext();
        validator.validateList(fromDate, toDate, page, limit, status, sourceType);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        Long total = repository.countReports(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                sourceType,
                department,
                search
        );

        List<DiagnosticReportResponse> rows = repository.findReports(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                sourceType,
                department,
                search,
                page,
                limit
        );

        return new PageResponse<>(
                rows,
                new PaginationMeta(page, limit, total)
        );
    }

    public DiagnosticReportResponse getReport(Long reportId) {
        validateContext();
        validator.validateReportId(reportId);

        return repository.findReportById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );
    }

    @Transactional
    public DiagnosticReportResponse createReport(CreateDiagnosticReportRequest request) {
        validateContext();
        validator.validateCreate(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        boolean reviewRequired = Boolean.TRUE.equals(request.expertReviewRequired())
                || Boolean.TRUE.equals(request.criticalFlag());

        String initialStatus = reviewRequired ? "ExpertReviewPending" : "Draft";
        String expertStatus = reviewRequired ? "Pending" : "NotRequired";
        String reportNo = "DRPT-" + System.currentTimeMillis();

        Long reportId = repository.createReport(
                tenantId,
                hospitalId,
                branchId,
                userId,
                request,
                reportNo,
                initialStatus,
                expertStatus
        );

        if (request.sections() != null) {
            for (ReportSectionRequest section : request.sections()) {
                repository.createSection(tenantId, hospitalId, branchId, reportId, section);
            }
        }

        if (request.observations() != null) {
            for (ReportObservationRequest observation : request.observations()) {
                repository.createObservation(tenantId, hospitalId, branchId, reportId, observation);
            }
        }

        repository.insertAudit(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                null,
                initialStatus,
                "REPORT_CREATED",
                null,
                "Diagnostic report created.",
                userId,
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return repository.findReportById(tenantId, hospitalId, branchId, reportId);
    }

    @Transactional
    public ExpertReviewResponse expertReview(Long reportId, ExpertReviewRequest request) {
        validateContext();
        validator.validateReportId(reportId);
        validator.validateExpertReview(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        DiagnosticReportResponse report = repository.findReportById(tenantId, hospitalId, branchId, reportId);

        if (!Boolean.TRUE.equals(report.expertReviewRequired())) {
            throw ApiException.workflow("Expert review is not required for this report.");
        }

        if ("Released".equals(report.reportStatus())) {
            throw ApiException.workflow("Released report cannot be reviewed.");
        }

        ExpertReviewResponse response = repository.expertReview(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                TenantContext.getUserId(),
                request
        );

        String toStatus = "Approved".equals(request.reviewStatus())
                ? "ExpertReviewed"
                : "ExpertReviewPending";

        repository.insertAudit(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                report.reportStatus(),
                toStatus,
                "EXPERT_REVIEW",
                request.reviewStatus(),
                request.comments(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    @Transactional
    public ReportSignatureResponse signReport(Long reportId, SignReportRequest request) {
        validateContext();
        validator.validateReportId(reportId);
        validator.validateSign(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        DiagnosticReportResponse report = repository.findReportById(tenantId, hospitalId, branchId, reportId);

        if ("Released".equals(report.reportStatus())) {
            throw ApiException.workflow("Released report cannot be signed again.");
        }

        if (Boolean.TRUE.equals(report.expertReviewRequired())
                && !"ExpertReviewed".equals(report.reportStatus())
                && !"Amended".equals(report.reportStatus())) {
            throw ApiException.workflow("Expert reviewed or amended report is required before signing.");
        }

        if (!Boolean.TRUE.equals(report.expertReviewRequired())
                && !"Draft".equals(report.reportStatus())
                && !"Amended".equals(report.reportStatus())) {
            throw ApiException.workflow("Only Draft or Amended report can be signed.");
        }

        ReportSignatureResponse response = repository.signReport(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                TenantContext.getUserId(),
                request
        );

        repository.insertAudit(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                report.reportStatus(),
                "Signed",
                "REPORT_SIGNED",
                null,
                "Report signed.",
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    @Transactional
    public ReportReleaseResponse releaseReport(Long reportId, ReleaseReportRequest request) {
        validateContext();
        validator.validateReportId(reportId);
        validator.validateRelease(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        DiagnosticReportResponse report = repository.findReportById(tenantId, hospitalId, branchId, reportId);

        if (!"Signed".equals(report.reportStatus())) {
            throw ApiException.workflow("Only Signed report can be released.");
        }

        ReportReleaseResponse response = repository.releaseReport(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                TenantContext.getUserId(),
                request
        );

        repository.insertAudit(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                report.reportStatus(),
                "Released",
                "REPORT_RELEASED",
                null,
                request.remarks(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    @Transactional
    public ReportAmendmentResponse amendReport(Long reportId, AmendReportRequest request) {
        validateContext();
        validator.validateReportId(reportId);
        validator.validateAmend(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        DiagnosticReportResponse report = repository.findReportById(tenantId, hospitalId, branchId, reportId);

        if (!"Released".equals(report.reportStatus())) {
            throw ApiException.workflow("Only Released report can be amended.");
        }

        ReportAmendmentResponse response = repository.amendReport(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                TenantContext.getUserId(),
                request
        );

        repository.insertAudit(
                tenantId,
                hospitalId,
                branchId,
                reportId,
                report.reportStatus(),
                "Amended",
                "REPORT_AMENDED",
                request.amendmentReason(),
                request.amendedContent(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    public List<ReportAuditResponse> getAudit(Long reportId) {
        validateContext();
        validator.validateReportId(reportId);

        repository.findReportById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );

        return repository.findAudit(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                reportId
        );
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }
}