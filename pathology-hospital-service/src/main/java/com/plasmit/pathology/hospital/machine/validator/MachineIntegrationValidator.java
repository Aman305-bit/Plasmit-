package com.plasmit.pathology.hospital.machine.validator;

import com.plasmit.pathology.hospital.common.exception.ApiException;
import com.plasmit.pathology.hospital.machine.dto.request.*;
import com.plasmit.pathology.hospital.validator.DateRangeValidator;
import com.plasmit.pathology.hospital.validator.PaginationValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class MachineIntegrationValidator {

    private static final Set<String> ALLOWED_PROCESSING_STATUS = Set.of(
            "Received",
            "Matched",
            "Imported",
            "Rejected"
    );

    private final DateRangeValidator dateRangeValidator;
    private final PaginationValidator paginationValidator;

    public MachineIntegrationValidator(DateRangeValidator dateRangeValidator,
                                       PaginationValidator paginationValidator) {
        this.dateRangeValidator = dateRangeValidator;
        this.paginationValidator = paginationValidator;
    }

    public void validateMachineId(Long machineId) {
        if (machineId == null || machineId <= 0) {
            throw ApiException.validation("Valid machineId is required.");
        }
    }

    public void validateMessageId(Long messageId) {
        if (messageId == null || messageId <= 0) {
            throw ApiException.validation("Valid messageId is required.");
        }
    }

    public void validateList(LocalDate fromDate,
                             LocalDate toDate,
                             Integer page,
                             Integer limit,
                             String processingStatus) {

        dateRangeValidator.validateMaxRangeDays(fromDate, toDate, 366);
        paginationValidator.validate(page, limit);

        if (processingStatus != null
                && !processingStatus.isBlank()
                && !ALLOWED_PROCESSING_STATUS.contains(processingStatus)) {
            throw ApiException.validation("Invalid processingStatus.");
        }
    }

    public void validateCreateMessage(MachineMessageCreateRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        validateMachineId(request.machineId());

        if ((request.barcodeNo() == null || request.barcodeNo().isBlank())
                && (request.machineSampleId() == null || request.machineSampleId().isBlank())) {
            throw ApiException.validation("barcodeNo or machineSampleId is required.");
        }

        if (request.parameters() == null || request.parameters().isEmpty()) {
            throw ApiException.validation("At least one machine result parameter is required.");
        }

        for (MachineMessageParameterRequest parameter : request.parameters()) {
            if (parameter.machineTestCode() == null || parameter.machineTestCode().isBlank()) {
                throw ApiException.validation("machineTestCode is required.");
            }

            if (parameter.resultValue() == null || parameter.resultValue().isBlank()) {
                throw ApiException.validation("resultValue is required.");
            }
        }
    }

    public void validateMatch(MachineMessageMatchRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.specimenId() == null || request.specimenId() <= 0) {
            throw ApiException.validation("Valid specimenId is required.");
        }
    }

    public void validateReject(MachineMessageRejectRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body is required.");
        }

        if (request.reason() == null || request.reason().isBlank()) {
            throw ApiException.validation("reason is required.");
        }
    }
}