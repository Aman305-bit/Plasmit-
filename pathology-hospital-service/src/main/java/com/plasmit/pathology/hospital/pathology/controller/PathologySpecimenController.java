package com.plasmit.pathology.hospital.pathology.controller;

import com.plasmit.pathology.hospital.common.response.ApiResponse;
import com.plasmit.pathology.hospital.common.response.PageResponse;
import com.plasmit.pathology.hospital.pathology.dto.request.*;
import com.plasmit.pathology.hospital.pathology.dto.response.*;
import com.plasmit.pathology.hospital.pathology.service.PathologySpecimenService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/hospital/pathology/specimens")
@CrossOrigin("*")
public class PathologySpecimenController {

    private final PathologySpecimenService service;

    public PathologySpecimenController(PathologySpecimenService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<PathologySpecimenResponse>> getSpecimens(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "modality", required = false)
            String modality,

            @RequestParam(value = "search", required = false)
            String search,

            @RequestParam(value = "page", defaultValue = "1")
            Integer page,

            @RequestParam(value = "limit", defaultValue = "25")
            Integer limit
    ) {
        return ApiResponse.success(
                "Pathology specimens fetched successfully.",
                service.getSpecimens(fromDate, toDate, status, modality, search, page, limit)
        );
    }

    @GetMapping("/{specimenId}")
    public ApiResponse<PathologySpecimenResponse> getSpecimen(
            @PathVariable Long specimenId
    ) {
        return ApiResponse.success(
                "Pathology specimen fetched successfully.",
                service.getSpecimen(specimenId)
        );
    }

    @PostMapping("/{specimenId}/collect")
    public ApiResponse<PathologySpecimenResponse> collect(
            @PathVariable Long specimenId,
            @Valid @RequestBody CollectSpecimenRequest request
    ) {
        return ApiResponse.success(
                "Pathology specimen collected successfully.",
                service.collect(specimenId, request)
        );
    }

    @PostMapping("/{specimenId}/receive")
    public ApiResponse<PathologySpecimenResponse> receive(
            @PathVariable Long specimenId,
            @Valid @RequestBody ReceiveSpecimenRequest request
    ) {
        return ApiResponse.success(
                "Pathology specimen received successfully.",
                service.receive(specimenId, request)
        );
    }

    @PostMapping("/{specimenId}/reject")
    public ApiResponse<PathologySpecimenResponse> reject(
            @PathVariable Long specimenId,
            @Valid @RequestBody RejectSpecimenRequest request
    ) {
        return ApiResponse.success(
                "Pathology specimen rejected successfully.",
                service.reject(specimenId, request)
        );
    }

    @PostMapping("/{specimenId}/result-entry")
    public ApiResponse<ResultEntryResponse> resultEntry(
            @PathVariable Long specimenId,
            @Valid @RequestBody ResultEntryRequest request
    ) {
        return ApiResponse.success(
                "Pathology result entered successfully.",
                service.enterResult(specimenId, request)
        );
    }

    @PostMapping("/{specimenId}/technical-verify")
    public ApiResponse<VerificationResponse> technicalVerify(
            @PathVariable Long specimenId,
            @Valid @RequestBody VerificationRequest request
    ) {
        return ApiResponse.success(
                "Pathology result technical verified successfully.",
                service.technicalVerify(specimenId, request)
        );
    }

    @PostMapping("/{specimenId}/clinical-verify")
    public ApiResponse<VerificationResponse> clinicalVerify(
            @PathVariable Long specimenId,
            @Valid @RequestBody VerificationRequest request
    ) {
        return ApiResponse.success(
                "Pathology result clinical verified successfully.",
                service.clinicalVerify(specimenId, request)
        );
    }
}