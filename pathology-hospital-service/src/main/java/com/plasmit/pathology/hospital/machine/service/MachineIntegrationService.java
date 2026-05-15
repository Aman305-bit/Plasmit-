package com.plasmit.pathology.hospital.machine.service;

import com.plasmit.pathology.hospital.common.response.PageResponse;
import com.plasmit.pathology.hospital.common.response.PaginationMeta;
import com.plasmit.pathology.hospital.context.TenantContext;
import com.plasmit.pathology.hospital.machine.dto.request.*;
import com.plasmit.pathology.hospital.machine.dto.response.*;
import com.plasmit.pathology.hospital.machine.repository.MachineIntegrationRepository;
import com.plasmit.pathology.hospital.machine.validator.MachineIntegrationValidator;
import com.plasmit.pathology.hospital.validator.CommonRequestValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MachineIntegrationService {

    private final MachineIntegrationRepository repository;
    private final MachineIntegrationValidator validator;
    private final CommonRequestValidator commonRequestValidator;

    public MachineIntegrationService(MachineIntegrationRepository repository,
                                     MachineIntegrationValidator validator,
                                     CommonRequestValidator commonRequestValidator) {
        this.repository = repository;
        this.validator = validator;
        this.commonRequestValidator = commonRequestValidator;
    }

    public List<PathologyMachineResponse> getMachines(String modality, String status) {
        validateContext();

        return repository.findMachines(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                modality,
                status
        );
    }

    @Transactional
    public MachineMessageResponse createMessage(MachineMessageCreateRequest request) {
        validateContext();
        validator.validateCreateMessage(request);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();
        Long userId = TenantContext.getUserId();

        repository.validateMachineExists(
                tenantId,
                hospitalId,
                branchId,
                request.machineId()
        );

        Long matchedSpecimenId = repository.autoMatchSpecimenByBarcode(
                tenantId,
                hospitalId,
                branchId,
                request.barcodeNo()
        );

        String matchStatus = matchedSpecimenId == null ? "Unmatched" : "Matched";
        String processingStatus = matchedSpecimenId == null ? "Received" : "Matched";

        Long messageId = repository.createMachineMessage(
                tenantId,
                hospitalId,
                branchId,
                userId,
                request,
                matchedSpecimenId,
                matchStatus,
                processingStatus
        );

        for (MachineMessageParameterRequest parameter : request.parameters()) {
            repository.createMessageParameter(
                    tenantId,
                    hospitalId,
                    branchId,
                    messageId,
                    request.machineId(),
                    parameter
            );
        }

        return repository.findMessageById(tenantId, hospitalId, branchId, messageId);
    }

    public PageResponse<MachineMessageResponse> getMessages(LocalDate fromDate,
                                                            LocalDate toDate,
                                                            String processingStatus,
                                                            String matchStatus,
                                                            Long machineId,
                                                            String search,
                                                            Integer page,
                                                            Integer limit) {

        validateContext();
        validator.validateList(fromDate, toDate, page, limit, processingStatus);

        Long tenantId = TenantContext.getTenantId();
        Long hospitalId = TenantContext.getHospitalId();
        String branchId = TenantContext.getBranchId();

        Long total = repository.countMessages(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                processingStatus,
                matchStatus,
                machineId,
                search
        );

        List<MachineMessageResponse> rows = repository.findMessages(
                tenantId,
                hospitalId,
                branchId,
                fromDate,
                toDate,
                processingStatus,
                matchStatus,
                machineId,
                search,
                page,
                limit
        );

        return new PageResponse<>(
                rows,
                new PaginationMeta(page, limit, total)
        );
    }

    public MachineMessageResponse getMessage(Long messageId) {
        validateContext();
        validator.validateMessageId(messageId);

        return repository.findMessageById(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                messageId
        );
    }

    @Transactional
    public MachineMessageResponse matchMessage(Long messageId,
                                               MachineMessageMatchRequest request) {
        validateContext();
        validator.validateMessageId(messageId);
        validator.validateMatch(request);

        repository.matchMessage(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                messageId,
                request.specimenId()
        );

        return getMessage(messageId);
    }

    @Transactional
    public MachineImportResponse importMessage(Long messageId) {
        validateContext();
        validator.validateMessageId(messageId);

        return repository.importMessage(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                messageId,
                TenantContext.getUserId()
        );
    }

    @Transactional
    public MachineMessageResponse rejectMessage(Long messageId,
                                                MachineMessageRejectRequest request) {
        validateContext();
        validator.validateMessageId(messageId);
        validator.validateReject(request);

        repository.rejectMessage(
                TenantContext.getTenantId(),
                TenantContext.getHospitalId(),
                TenantContext.getBranchId(),
                messageId,
                request.reason()
        );

        return getMessage(messageId);
    }

    private void validateContext() {
        commonRequestValidator.validateTenantContext();
        commonRequestValidator.validateHospitalContext();
        commonRequestValidator.validateUserContext();
        commonRequestValidator.validateBranchRequired();
    }
}