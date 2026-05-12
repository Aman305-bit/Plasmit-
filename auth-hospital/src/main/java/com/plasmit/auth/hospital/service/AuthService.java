package com.plasmit.auth.hospital.service;

import java.util.List;
import java.util.Map;

import com.plasmit.auth.hospital.exception.ApiException;
import com.plasmit.auth.hospital.repository.UserRepository;
import com.plasmit.auth.hospital.repository.UserRepository.UserRecord;
import com.plasmit.auth.hospital.security.SecurityConfig.CurrentUser;
import com.plasmit.auth.hospital.security.SecurityConfig.TenantContext;
import com.plasmit.auth.hospital.validator.AuthValidator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthValidator authValidator;

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AuthValidator authValidator
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authValidator = authValidator;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {

        String email = request.email().trim().toLowerCase();

        log.info("Login request received. email={}", email);

        UserRecord user = userRepository.findActiveUserByEmail(email).orElse(null);

        try {
            authValidator.validateLoginUser(user, email);
            authValidator.validatePassword(request.password(), user);
        } catch (ApiException ex) {

            userRepository.saveLoginActivity(
                    user == null ? null : user.tenantId(),
                    user == null ? null : user.hospitalId(),
                    user == null ? null : user.id(),
                    email,
                    "FAILED",
                    "INVALID_CREDENTIALS",
                    getClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent")
            );

            throw ex;
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        userRepository.updateLastLogin(user.id());

        userRepository.saveLoginActivity(
                user.tenantId(),
                user.hospitalId(),
                user.id(),
                email,
                "SUCCESS",
                null,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        userRepository.saveAuditLog(
                user.tenantId(),
                user.hospitalId(),
                user.branchId(),
                user.id(),
                "AUTH_LOGIN_SUCCESS",
                "Hospital user logged in successfully.",
                MDC.get("requestId"),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        MDC.put("tenantId", String.valueOf(user.tenantId()));
        MDC.put("hospitalId", String.valueOf(user.hospitalId()));
        MDC.put("branchId", String.valueOf(user.branchId()));
        MDC.put("userId", String.valueOf(user.id()));

        log.info("Login successful. userId={} tenantId={} hospitalId={} branchId={}",
                user.id(), user.tenantId(), user.hospitalId(), user.branchId());

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                new UserSummary(
                        user.id(),
                        user.fullName(),
                        user.email(),
                        user.tenantId(),
                        user.hospitalId(),
                        user.branchId(),
                        user.departmentId(),
                        user.hospitalName(),
                        user.branchName(),
                        user.departmentName(),
                        user.roleCode(),
                        user.permissions()
                )
        );
    }

    @Transactional
    public void resetPasswordByEmail(String email, String newPassword, Long updatedBy) {

        authValidator.validatePasswordResetRequest(email, newPassword);

        String normalizedEmail = email.trim().toLowerCase();
        String passwordHash = passwordEncoder.encode(newPassword);

        userRepository.resetPasswordByEmail(normalizedEmail, passwordHash, updatedBy);

        log.info("Password reset successfully. email={}", normalizedEmail);
    }

    public void logout(HttpServletRequest httpRequest) {

        CurrentUser currentUser = TenantContext.get();

        if (currentUser == null) {
            throw ApiException.unauthorized("Authentication required.");
        }

        log.info("Logout request received. userId={} tenantId={}",
                currentUser.getUserId(), currentUser.getTenantId());

        userRepository.saveAuditLog(
                currentUser.getTenantId(),
                currentUser.getHospitalId(),
                currentUser.getBranchId(),
                currentUser.getUserId(),
                "AUTH_LOGOUT",
                "Hospital user logged out.",
                MDC.get("requestId"),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );
    }

    public CurrentUserResponse currentUser() {

        CurrentUser currentUser = TenantContext.get();

        if (currentUser == null) {
            throw ApiException.unauthorized("Authentication required.");
        }

        log.info("Current user requested. userId={} tenantId={} hospitalId={}",
                currentUser.getUserId(), currentUser.getTenantId(), currentUser.getHospitalId());

        UserRecord user = userRepository.findActiveUserByIdAndTenant(
                currentUser.getUserId(),
                currentUser.getTenantId(),
                currentUser.getHospitalId()
        ).orElseThrow(() -> ApiException.unauthorized("User session is no longer valid."));

        return new CurrentUserResponse(
                user.id(),
                user.fullName(),
                user.email(),
                user.phone(),
                user.tenantId(),
                user.hospitalId(),
                user.branchId(),
                user.departmentId(),
                user.hospitalName(),
                user.branchName(),
                user.departmentName(),
                user.roleCode(),
                user.permissions(),
                Map.of(
                        "themeMode", "light",
                        "sidebarColor", "hospitalTeal",
                        "topbarColor", "hospitalBlue"
                ),
                user.lastLoginAt()
        );
    }

    public NavigationResponse navigation() {

        CurrentUser currentUser = TenantContext.get();

        if (currentUser == null) {
            throw ApiException.unauthorized("Authentication required.");
        }

        log.info("Navigation requested. userId={} tenantId={}",
                currentUser.getUserId(), currentUser.getTenantId());

        List<NavigationItem> allItems = List.of(
                new NavigationItem("Dashboard", "/", "dashboard.view"),
                new NavigationItem("Billing Desk", "/billing-desk", "billing.view"),
                new NavigationItem("Appointments", "/appointments", "appointments.view"),
                new NavigationItem("Patients", "/patients", "patients.view"),
                new NavigationItem("Doctors", "/doctors", "doctors.view"),
                new NavigationItem("Test Catalog", "/test-catalog", "test_catalog.view"),
                new NavigationItem("Test Packages", "/test-packages", "test_packages.view"),
                new NavigationItem("Staff", "/staff", "staff.view"),
                new NavigationItem("Settings", "/settings", "settings.view")
        );

        List<NavigationItem> allowedItems = allItems.stream()
                .filter(item -> currentUser.hasPermission(item.permission()))
                .toList();

        return new NavigationResponse(
                allowedItems,
                Map.of(
                        "sidebarBackground", "#0f766e",
                        "topbarBackground", "#eff6ff"
                )
        );
    }

    private String getClientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    public record LoginRequest(

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            UserSummary user
    ) {
    }

    public record UserSummary(
            Long id,
            String name,
            String email,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            String hospitalName,
            String branchName,
            String departmentName,
            String role,
            List<String> permissions
    ) {
    }

    public record CurrentUserResponse(
            Long id,
            String name,
            String email,
            String phone,
            Long tenantId,
            Long hospitalId,
            Long branchId,
            Long departmentId,
            String hospitalName,
            String branchName,
            String departmentName,
            String role,
            List<String> permissions,
            Map<String, Object> preferences,
            String lastLoginAt
    ) {
    }

    public record NavigationResponse(
            List<NavigationItem> items,
            Map<String, Object> theme
    ) {
    }

    public record NavigationItem(
            String label,
            String href,
            String permission
    ) {
    }
}