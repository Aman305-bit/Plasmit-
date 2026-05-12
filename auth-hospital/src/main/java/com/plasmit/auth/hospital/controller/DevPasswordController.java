package com.plasmit.auth.hospital.controller;

import com.plasmit.auth.hospital.common.ApiResponse;
import com.plasmit.auth.hospital.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/dev")
public class DevPasswordController {

    private final AuthService authService;

    public DevPasswordController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {

        authService.resetPasswordByEmail(
                request.email(),
                request.newPassword(),
                request.updatedBy()
        );

        return ApiResponse.success("Password reset successfully", null);
    }

    public record ResetPasswordRequest(
            String email,
            String newPassword,
            Long updatedBy
    ) {
    }
}