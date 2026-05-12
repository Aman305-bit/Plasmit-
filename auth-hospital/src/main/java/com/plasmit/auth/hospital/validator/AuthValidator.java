package com.plasmit.auth.hospital.validator;

import com.plasmit.auth.hospital.exception.ApiException;
import com.plasmit.auth.hospital.repository.UserRepository.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AuthValidator {

    private static final Logger log = LoggerFactory.getLogger(AuthValidator.class);

    private final PasswordEncoder passwordEncoder;

    public AuthValidator(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void validateLoginUser(UserRecord user, String email) {

        if (user == null) {
            log.warn("Login validation failed. reason=USER_NOT_FOUND email={}", email);
            throw ApiException.unauthorized("Invalid email or password.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            log.warn(
                    "Login validation failed. reason=USER_INACTIVE userId={} tenantId={} hospitalId={}",
                    user.id(),
                    user.tenantId(),
                    user.hospitalId()
            );
            throw ApiException.unauthorized("Invalid email or password.");
        }

        if (user.tenantId() == null || user.hospitalId() == null) {
            log.warn(
                    "Login validation failed. reason=TENANT_OR_HOSPITAL_MISSING userId={}",
                    user.id()
            );
            throw ApiException.unauthorized("Invalid email or password.");
        }

        if (user.roleCode() == null || user.roleCode().isBlank()) {
            log.warn(
                    "Login validation failed. reason=ROLE_MAPPING_MISSING userId={} tenantId={} hospitalId={}",
                    user.id(),
                    user.tenantId(),
                    user.hospitalId()
            );
            throw ApiException.unauthorized("Invalid email or password.");
        }
    }

    public void validatePassword(String rawPassword, UserRecord user) {

        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            log.warn(
                    "Login validation failed. reason=BAD_PASSWORD userId={} tenantId={} hospitalId={}",
                    user.id(),
                    user.tenantId(),
                    user.hospitalId()
            );
            throw ApiException.unauthorized("Invalid email or password.");
        }
    }

    public void validatePasswordResetRequest(String email, String newPassword) {

        if (email == null || email.isBlank()) {
            log.warn("Password reset validation failed. reason=EMAIL_REQUIRED");
            throw ApiException.unauthorized("Email is required.");
        }

        if (newPassword == null || newPassword.isBlank()) {
            log.warn("Password reset validation failed. reason=PASSWORD_REQUIRED email={}", email);
            throw ApiException.unauthorized("Password is required.");
        }

        if (newPassword.length() < 8) {
            log.warn("Password reset validation failed. reason=PASSWORD_TOO_SHORT email={}", email);
            throw ApiException.unauthorized("Password must be at least 8 characters.");
        }
    }
}