package com.plasmit.radiology.hospital.radiology.controller;

import com.plasmit.radiology.hospital.common.response.ApiResponse;
import com.plasmit.radiology.hospital.radiology.dto.request.PacsMatchRequest;
import com.plasmit.radiology.hospital.radiology.dto.request.RadiologyScheduleRequest;
import com.plasmit.radiology.hospital.radiology.dto.request.SafetyChecklistRequest;
import com.plasmit.radiology.hospital.radiology.dto.response.*;
import com.plasmit.radiology.hospital.radiology.service.RadiologyStudyService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hospital/radiology/studies")
@CrossOrigin("*")
public class RadiologyStudyController {

    private final RadiologyStudyService service;

    public RadiologyStudyController(RadiologyStudyService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getStudies(
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
                "Radiology studies fetched successfully.",
                service.getStudies(fromDate, toDate, status, modality, search, page, limit)
        );
    }

    @GetMapping("/{studyId}")
    public ApiResponse<RadiologyStudyResponse> getStudy(@PathVariable Long studyId) {
        return ApiResponse.success(
                "Radiology study fetched successfully.",
                service.getStudy(studyId)
        );
    }

    @PostMapping("/{studyId}/schedule")
    public ApiResponse<RadiologyScheduleResponse> schedule(
            @PathVariable Long studyId,
            @Valid @RequestBody RadiologyScheduleRequest request
    ) {
        return ApiResponse.success(
                "Radiology study scheduled successfully.",
                service.schedule(studyId, request)
        );
    }

    @PostMapping("/{studyId}/safety-checklist")
    public ApiResponse<SafetyChecklistResponse> safetyChecklist(
            @PathVariable Long studyId,
            @Valid @RequestBody SafetyChecklistRequest request
    ) {
        return ApiResponse.success(
                "Radiology safety checklist saved successfully.",
                service.saveSafetyChecklist(studyId, request)
        );
    }

    @PostMapping("/{studyId}/pacs-match")
    public ApiResponse<PacsMatchResponse> pacsMatch(
            @PathVariable Long studyId,
            @Valid @RequestBody PacsMatchRequest request
    ) {
        return ApiResponse.success(
                "Radiology PACS match completed successfully.",
                service.matchPacs(studyId, request)
        );
    }

    @GetMapping("/{studyId}/viewer-launch")
    public ApiResponse<ViewerLaunchResponse> viewerLaunch(@PathVariable Long studyId) {
        return ApiResponse.success(
                "Radiology viewer launch URL fetched successfully.",
                service.launchViewer(studyId)
        );
    }
}