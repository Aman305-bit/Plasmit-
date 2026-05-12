package com.plasmit.billing.hospital.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import com.plasmit.billing.hospital.security.SecurityConfig.CurrentUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public CurrentUser validateAccessToken(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get("tokenType", String.class);

        if (!"ACCESS".equals(tokenType)) {
            throw new IllegalArgumentException("Invalid token type.");
        }

        Long userId = getLongClaim(claims, "userId");
        Long tenantId = getLongClaim(claims, "tenantId");
        Long hospitalId = getLongClaim(claims, "hospitalId");
        Long branchId = getLongClaim(claims, "branchId");
        Long departmentId = getLongClaim(claims, "departmentId");

        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get("permissions", List.class);

        log.info("Access token verified. userId={} tenantId={} hospitalId={}",
                userId, tenantId, hospitalId);

        return new CurrentUser(
                userId,
                tenantId,
                hospitalId,
                branchId,
                departmentId,
                claims.get("name", String.class),
                claims.get("email", String.class),
                claims.get("role", String.class),
                permissions
        );
    }

    private Long getLongClaim(Claims claims, String key) {

        Object value = claims.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }

        if (value instanceof Long longValue) {
            return longValue;
        }

        return Long.valueOf(value.toString());
    }
}