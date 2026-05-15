package com.plasmit.radiology.hospital.radiology.service;

import com.plasmit.radiology.hospital.common.exception.ApiException;
import com.plasmit.radiology.hospital.context.TenantContext;
import com.plasmit.radiology.hospital.radiology.dto.request.PacsMatchRequest;
import com.plasmit.radiology.hospital.radiology.dto.request.RadiologyScheduleRequest;
import com.plasmit.radiology.hospital.radiology.dto.request.SafetyChecklistRequest;
import com.plasmit.radiology.hospital.radiology.dto.response.*;
import com.plasmit.radiology.hospital.radiology.repository.RadiologyStudyRepository;
import com.plasmit.radiology.hospital.radiology.validator.RadiologyStudyValidator;
import com.plasmit.radiology.hospital.validator.CommonRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RadiologyStudyService {

    private static final Logger log = LoggerFactory.getLogger(RadiologyStudyService.class);

    private final RadiologyStudyRepository repository;
    private final CommonRequestValidator commonRequestValidator;
    private final RadiologyStudyValidator validator;

    public RadiologyStudyService(RadiologyStudyRepository repository,
                                 CommonRequestValidator commonRequestValidator,
                                 RadiologyStudyValidator validator) {
        this.repository = repository;
        this.commonRequestValidator = commonRequestValidator;
        this.validator = validator;
    }

    @Transactional
    public Map<String, Object> getStudies(LocalDate fromDate,
                                          LocalDate toDate,
                                          String status,
                                          String modality,
                                          String search,
                                          Integer page,
                                          Integer limit) {

        validateContext();
        validator.validateStudyList(fromDate, toDate, page, limit);
        validator.validateStatus(status);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        repository.syncRadiologyStudiesFromDiagnosticOrders(
                tenantId,
                hospitalId,
                branchId,
                TenantContext.getUserId()
        );

        Long total = repository.countStudies(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                modality,
                search
        );

        List<RadiologyStudyResponse> rows = repository.findStudies(
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

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", total);
        pagination.put("totalPages", (int) Math.ceil((double) total / limit));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("rows", rows);
        response.put("pagination", pagination);

        return response;
    }

    public RadiologyStudyResponse getStudy(Long studyId) {
        validateContext();
        validator.validateStudyId(studyId);

        return repository.findStudyById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                studyId
        );
    }

    @Transactional
    public RadiologyScheduleResponse schedule(Long studyId, RadiologyScheduleRequest request) {
        validateContext();
        validator.validateStudyId(studyId);
        validator.validateSchedule(request);

        log.info("Scheduling radiology study. studyId={} scheduledAt={}", studyId, request.scheduledAt());

        return repository.scheduleStudy(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                studyId,
                request,
                TenantContext.getUserId()
        );
    }

    @Transactional
    public SafetyChecklistResponse saveSafetyChecklist(Long studyId, SafetyChecklistRequest request) {
        validateContext();
        validator.validateStudyId(studyId);
        validator.validateSafetyChecklist(request);

        return repository.saveSafetyChecklist(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                studyId,
                request,
                TenantContext.getUserId()
        );
    }

    @Transactional
    public PacsMatchResponse matchPacs(Long studyId, PacsMatchRequest request) {
        validateContext();
        validator.validateStudyId(studyId);
        validator.validatePacsMatch(request);

        return repository.matchPacs(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                studyId,
                request,
                TenantContext.getUserId()
        );
    }

    @Transactional
    public ViewerLaunchResponse launchViewer(Long studyId) {
        validateContext();
        validator.validateStudyId(studyId);

        return repository.launchViewer(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                studyId,
                TenantContext.getUserId(),
                TenantContext.getRequestId()
        );
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }
}