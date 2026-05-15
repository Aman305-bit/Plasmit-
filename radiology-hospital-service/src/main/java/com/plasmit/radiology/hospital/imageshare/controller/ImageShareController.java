package com.plasmit.radiology.hospital.imageshare.controller;

import com.plasmit.radiology.hospital.common.response.ApiResponse;
import com.plasmit.radiology.hospital.imageshare.dto.request.CreateImageShareRequest;
import com.plasmit.radiology.hospital.imageshare.dto.request.RevokeImageShareRequest;
import com.plasmit.radiology.hospital.imageshare.dto.response.ImageShareAccessLogResponse;
import com.plasmit.radiology.hospital.imageshare.dto.response.ImageShareResponse;
import com.plasmit.radiology.hospital.imageshare.service.ImageShareService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hospital/radiology/image-shares")
@CrossOrigin("*")
public class ImageShareController {

    private final ImageShareService service;

    public ImageShareController(ImageShareService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getShares(
            @RequestParam("fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam("toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(value = "status", required = false)
            String status,

            @RequestParam(value = "studyId", required = false)
            Long studyId,

            @RequestParam(value = "search", required = false)
            String search,

            @RequestParam(value = "page", defaultValue = "1")
            Integer page,

            @RequestParam(value = "limit", defaultValue = "25")
            Integer limit
    ) {
        return ApiResponse.success(
                "Radiology image shares fetched successfully.",
                service.getShares(fromDate, toDate, status, studyId, search, page, limit)
        );
    }

    @PostMapping
    public ApiResponse<ImageShareResponse> createShare(
            @Valid @RequestBody CreateImageShareRequest request
    ) {
        return ApiResponse.success(
                "Radiology image share created successfully.",
                service.createShare(request)
        );
    }

    @PostMapping("/{shareId}/revoke")
    public ApiResponse<ImageShareResponse> revokeShare(
            @PathVariable Long shareId,
            @Valid @RequestBody RevokeImageShareRequest request
    ) {
        return ApiResponse.success(
                "Radiology image share revoked successfully.",
                service.revokeShare(shareId, request)
        );
    }

    @GetMapping("/{shareId}/access-logs")
    public ApiResponse<List<ImageShareAccessLogResponse>> getAccessLogs(
            @PathVariable Long shareId
    ) {
        return ApiResponse.success(
                "Radiology image share access logs fetched successfully.",
                service.getAccessLogs(shareId)
        );
    }
}