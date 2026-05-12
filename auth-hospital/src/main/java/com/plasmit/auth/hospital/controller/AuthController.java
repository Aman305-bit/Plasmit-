package com.plasmit.auth.hospital.controller;

import com.plasmit.auth.hospital.common.ApiResponse;
import com.plasmit.auth.hospital.service.AuthService;
import com.plasmit.auth.hospital.service.AuthService.CurrentUserResponse;
import com.plasmit.auth.hospital.service.AuthService.LoginRequest;
import com.plasmit.auth.hospital.service.AuthService.LoginResponse;
import com.plasmit.auth.hospital.service.AuthService.NavigationResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginHttpRequest request,
            HttpServletRequest httpRequest
    ) {

        log.info("Login API called.");

        LoginResponse response = authService.login(
                new LoginRequest(request.email(), request.password()),
                httpRequest
        );

        log.info("Login API completed.");

        return ResponseEntity.ok(ApiResponse.success("Login successful.", response));
    }

    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {

        log.info("Logout API called.");

        authService.logout(httpRequest);

        log.info("Logout API completed.");

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", null));
    }

    @GetMapping("/api/v1/hospital/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me() {

        log.info("Current hospital user API called.");

        CurrentUserResponse response = authService.currentUser();

        log.info("Current hospital user API completed. userId={}", response.id());

        return ResponseEntity.ok(ApiResponse.success("Current user fetched successfully.", response));
    }

    @GetMapping("/api/v1/hospital/navigation")
    public ResponseEntity<ApiResponse<NavigationResponse>> navigation() {

        log.info("Hospital navigation API called.");

        NavigationResponse response = authService.navigation();

        log.info("Hospital navigation API completed.");

        return ResponseEntity.ok(ApiResponse.success("Navigation fetched successfully.", response));
    }

    public record LoginHttpRequest(
            @NotBlank(message = "Email is required.")
            @Email(message = "Email must be valid.")
            String email,

            @NotBlank(message = "Password is required.")
            @Size(min = 8, message = "Password must be at least 8 characters.")
            String password
    ) {
    }
}