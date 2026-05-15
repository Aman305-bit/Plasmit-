package com.plasmit.pathology.hospital.pathology.service;

import com.plasmit.pathology.hospital.common.exception.ApiException;
import com.plasmit.pathology.hospital.common.response.PageResponse;
import com.plasmit.pathology.hospital.common.response.PaginationMeta;
import com.plasmit.pathology.hospital.context.TenantContext;
import com.plasmit.pathology.hospital.pathology.dto.request.*;
import com.plasmit.pathology.hospital.pathology.dto.response.*;
import com.plasmit.pathology.hospital.pathology.repository.PathologySpecimenRepository;
import com.plasmit.pathology.hospital.pathology.validator.PathologySpecimenValidator;
import com.plasmit.pathology.hospital.validator.CommonRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PathologySpecimenService {

    private static final Logger log = LoggerFactory.getLogger(PathologySpecimenService.class);

    private final PathologySpecimenRepository repository;
    private final PathologySpecimenValidator validator;
    private final CommonRequestValidator commonRequestValidator;

    public PathologySpecimenService(PathologySpecimenRepository repository,
                                    PathologySpecimenValidator validator,
                                    CommonRequestValidator commonRequestValidator) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
    }

    @Transactional
    public PageResponse<PathologySpecimenResponse> getSpecimens(LocalDate fromDate,
                                                                LocalDate toDate,
                                                                String status,
                                                                String modality,
                                                                String search,
                                                                Integer page,
                                                                Integer limit) {

        validateContext();
        validator.validateList(fromDate, toDate, page, limit, status);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        repository.syncSpecimensFromDiagnosticOrders(
                tenantId,
                hospitalId,
                branchId,
                TenantContext.getUserId()
        );

        Long total = repository.countSpecimens(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                modality,
                search
        );

        List<PathologySpecimenResponse> rows = repository.findSpecimens(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                modality,
                search,
                page,
                limit
        );

        return new PageResponse<>(
                rows,
                new PaginationMeta(page, limit, total)
        );
    }

    public PathologySpecimenResponse getSpecimen(Long specimenId) {
        validateContext();
        validator.validateSpecimenId(specimenId);

        return repository.findSpecimenById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId
        );
    }

    @Transactional
    public PathologySpecimenResponse collect(Long specimenId,
                                             CollectSpecimenRequest request) {

        validateContext();
        validator.validateSpecimenId(specimenId);
        validator.validateCollect(request);

        PathologySpecimenResponse existing = getSpecimen(specimenId);

        if (!"SamplePending".equals(existing.status())) {
            throw ApiException.workflow("Only SamplePending specimen can be collected.");
        }

        repository.updateCollect(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                TenantContext.getUserId(),
                request.collectedByName()
        );

        repository.insertEvent(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                existing.status(),
                "Collected",
                "SPECIMEN_COLLECTED",
                null,
                request.notes(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return getSpecimen(specimenId);
    }

    @Transactional
    public PathologySpecimenResponse receive(Long specimenId,
                                             ReceiveSpecimenRequest request) {

        validateContext();
        validator.validateSpecimenId(specimenId);
        validator.validateReceive(request);

        PathologySpecimenResponse existing = getSpecimen(specimenId);

        if (!"Collected".equals(existing.status())) {
            throw ApiException.workflow("Only Collected specimen can be received.");
        }

        repository.updateReceive(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                TenantContext.getUserId(),
                request.receivedByName()
        );

        repository.insertEvent(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                existing.status(),
                "Received",
                "SPECIMEN_RECEIVED",
                null,
                request.notes(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return getSpecimen(specimenId);
    }

    @Transactional
    public PathologySpecimenResponse reject(Long specimenId,
                                            RejectSpecimenRequest request) {

        validateContext();
        validator.validateSpecimenId(specimenId);
        validator.validateReject(request);

        PathologySpecimenResponse existing = getSpecimen(specimenId);

        if ("ClinicalVerified".equals(existing.status()) || "Released".equals(existing.status())) {
            throw ApiException.workflow("Verified or released specimen cannot be rejected.");
        }

        repository.updateReject(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                TenantContext.getUserId(),
                request.reason()
        );

        repository.insertEvent(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                existing.status(),
                "Rejected",
                "SPECIMEN_REJECTED",
                request.reason(),
                request.notes(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return getSpecimen(specimenId);
    }

    @Transactional
    public ResultEntryResponse enterResult(Long specimenId,
                                           ResultEntryRequest request) {

        validateContext();
        validator.validateSpecimenId(specimenId);
        validator.validateResultEntry(request);

        PathologySpecimenResponse existing = getSpecimen(specimenId);

        if (!"Received".equals(existing.status()) && !"InProcess".equals(existing.status())) {
            throw ApiException.workflow("Only Received or InProcess specimen can have result entry.");
        }

        Long resultEntryId = repository.createResultEntry(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                TenantContext.getUserId(),
                request
        );

        for (ResultParameterRequest parameter : request.parameters()) {
            repository.createResultParameter(
                    TenantContext.getTenantId(),
                    TenantContext.getHospitalId(),
                    TenantContext.getBranchId(),
                    resultEntryId,
                    specimenId,
                    parameter
            );
        }

        repository.markResultEntered(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                TenantContext.getUserId()
        );

        repository.insertEvent(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                existing.status(),
                "ResultEntered",
                "RESULT_ENTERED",
                null,
                request.resultSummary(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return repository.findLatestResultEntry(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId
        );
    }

    @Transactional
    public VerificationResponse technicalVerify(Long specimenId,
                                                VerificationRequest request) {

        validateContext();
        validator.validateSpecimenId(specimenId);
        validator.validateVerification(request);

        PathologySpecimenResponse existing = getSpecimen(specimenId);

        if (!"ResultEntered".equals(existing.status())) {
            throw ApiException.workflow("Only ResultEntered specimen can be technical verified.");
        }

        ResultEntryResponse result = repository.findLatestResultEntry(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId
        );

        VerificationResponse response = repository.createVerification(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                result.resultEntryId(),
                "Technical",
                TenantContext.getUserId(),
                request.verifierName(),
                request.remarks()
        );

        repository.updateVerificationStatus(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                result.resultEntryId(),
                "Technical",
                TenantContext.getUserId()
        );

        repository.insertEvent(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                existing.status(),
                "TechnicalVerified",
                "TECHNICAL_VERIFIED",
                null,
                request.remarks(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    @Transactional
    public VerificationResponse clinicalVerify(Long specimenId,
                                               VerificationRequest request) {

        validateContext();
        validator.validateSpecimenId(specimenId);
        validator.validateVerification(request);

        PathologySpecimenResponse existing = getSpecimen(specimenId);

        if (!"TechnicalVerified".equals(existing.status())) {
            throw ApiException.workflow("Only TechnicalVerified specimen can be clinical verified.");
        }

        ResultEntryResponse result = repository.findLatestResultEntry(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId
        );

        VerificationResponse response = repository.createVerification(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                result.resultEntryId(),
                "Clinical",
                TenantContext.getUserId(),
                request.verifierName(),
                request.remarks()
        );

        repository.updateVerificationStatus(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                result.resultEntryId(),
                "Clinical",
                TenantContext.getUserId()
        );

        repository.insertEvent(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                specimenId,
                existing.status(),
                "ClinicalVerified",
                "CLINICAL_VERIFIED",
                null,
                request.remarks(),
                TenantContext.getUserId(),
                TenantContext.getRole(),
                TenantContext.getRequestId()
        );

        return response;
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }
}