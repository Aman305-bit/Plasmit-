package com.plasmit.pathology.hospital.machine.controller;

import com.plasmit.pathology.hospital.common.response.ApiResponse;
import com.plasmit.pathology.hospital.common.response.PageResponse;
import com.plasmit.pathology.hospital.machine.dto.request.*;
import com.plasmit.pathology.hospital.machine.dto.response.*;
import com.plasmit.pathology.hospital.machine.service.MachineIntegrationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital/pathology/machine")
@CrossOrigin("*")
public class MachineIntegrationController {

    private final MachineIntegrationService service;

    public MachineIntegrationController(MachineIntegrationService service) {
        this.service = service;
    }

    @GetMapping("/machines")
    public ApiResponse<List<PathologyMachineResponse>> getMachines(
            @RequestParam(value = "modality", required = false) String modality,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.success(
                "Pathology machines fetched successfully.",
                service.getMachines(modality, status)
        );
    }

    @PostMapping("/messages")
    public ApiResponse<MachineMessageResponse> createMessage(
            @Valid @RequestBody MachineMessageCreateRequest request
    ) {
        return ApiResponse.success(
                "Machine result message received successfully.",
                service.createMessage(request)
        );
    }

    @GetMapping("/messages")
    public ApiResponse<PageResponse<MachineMessageResponse>> getMessages(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "processingStatus", required = false)
            String processingStatus,

            @RequestParam(value = "matchStatus", required = false)
            String matchStatus,

            @RequestParam(value = "machineId", required = false)
            Long machineId,

            @RequestParam(value = "search", required = false)
            String search,

            @RequestParam(value = "page", defaultValue = "1")
            Integer page,

            @RequestParam(value = "limit", defaultValue = "25")
            Integer limit
    ) {
        return ApiResponse.success(
                "Machine result messages fetched successfully.",
                service.getMessages(
                        fromDate,
                        toDate,
                        processingStatus,
                        matchStatus,
                        machineId,
                        search,
                        page,
                        limit
                )
        );
    }

    @GetMapping("/messages/{messageId}")
    public ApiResponse<MachineMessageResponse> getMessage(
            @PathVariable Long messageId
    ) {
        return ApiResponse.success(
                "Machine result message fetched successfully.",
                service.getMessage(messageId)
        );
    }

    @PostMapping("/messages/{messageId}/match")
    public ApiResponse<MachineMessageResponse> matchMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody MachineMessageMatchRequest request
    ) {
        return ApiResponse.success(
                "Machine result message matched successfully.",
                service.matchMessage(messageId, request)
        );
    }

    @PostMapping("/messages/{messageId}/import")
    public ApiResponse<MachineImportResponse> importMessage(
            @PathVariable Long messageId
    ) {
        return ApiResponse.success(
                "Machine result imported successfully.",
                service.importMessage(messageId)
        );
    }

    @PostMapping("/messages/{messageId}/reject")
    public ApiResponse<MachineMessageResponse> rejectMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody MachineMessageRejectRequest request
    ) {
        return ApiResponse.success(
                "Machine result message rejected successfully.",
                service.rejectMessage(messageId, request)
        );
    }
}