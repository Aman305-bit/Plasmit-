package com.plasmit.radiology.hospital.imageshare.service;

import com.plasmit.radiology.hospital.context.TenantContext;
import com.plasmit.radiology.hospital.imageshare.dto.request.CreateImageShareRequest;
import com.plasmit.radiology.hospital.imageshare.dto.request.RevokeImageShareRequest;
import com.plasmit.radiology.hospital.imageshare.dto.response.ImageShareAccessLogResponse;
import com.plasmit.radiology.hospital.imageshare.dto.response.ImageShareResponse;
import com.plasmit.radiology.hospital.imageshare.repository.ImageShareRepository;
import com.plasmit.radiology.hospital.imageshare.validator.ImageShareValidator;
import com.plasmit.radiology.hospital.validator.CommonRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImageShareService {

    private static final Logger log = LoggerFactory.getLogger(ImageShareService.class);

    private final ImageShareRepository repository;
    private final ImageShareValidator validator;
    private final CommonRequestValidator commonRequestValidator;

    public ImageShareService(ImageShareRepository repository,
                             ImageShareValidator validator,
                             CommonRequestValidator commonRequestValidator) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
    }

    @Transactional
    public Map<String, Object> getShares(LocalDate fromDate,
                                         LocalDate toDate,
                                         String status,
                                         Long studyId,
                                         String search,
                                         Integer page,
                                         Integer limit) {

        validateContext();
        validator.validateList(fromDate, toDate, page, limit, status);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        repository.expireOldShares();

        Long total = repository.countShares(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                studyId,
                search
        );

        List<ImageShareResponse> rows = repository.findShares(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                status,
                studyId,
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

    @Transactional
    public ImageShareResponse createShare(CreateImageShareRequest request) {

        validateContext();
        validator.validateCreate(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        String shareCode = generateShareCode();
        String secureToken = generateSecureToken();
        String shareUrl = "https://viewer.plasmit.local/share/radiology/" + secureToken;

        log.info("Creating radiology image share. tenantId={} hospitalId={} branchId={} studyId={} shareCode={}",
                tenantId, hospitalId, branchId, request.studyId(), shareCode);

        return repository.createShare(
                tenantId,
                hospitalId,
                branchId,
                userId,
                request,
                shareCode,
                secureToken,
                shareUrl
        );
    }

    @Transactional
    public ImageShareResponse revokeShare(Long shareId,
                                          RevokeImageShareRequest request) {

        validateContext();
        validator.validateShareId(shareId);
        validator.validateRevoke(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        log.info("Revoking radiology image share. tenantId={} hospitalId={} branchId={} shareId={}",
                tenantId, hospitalId, branchId, shareId);

        return repository.revokeShare(
                tenantId,
                hospitalId,
                branchId,
                shareId,
                userId,
                request.reason()
        );
    }

    public List<ImageShareAccessLogResponse> getAccessLogs(Long shareId) {

        validateContext();
        validator.validateShareId(shareId);

        return repository.findAccessLogs(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                shareId
        );
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }

    private String generateShareCode() {
        return "RSH-" + System.currentTimeMillis();
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}