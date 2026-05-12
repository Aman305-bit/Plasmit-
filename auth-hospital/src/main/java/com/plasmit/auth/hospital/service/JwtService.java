package com.plasmit.auth.hospital.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import com.plasmit.auth.hospital.repository.UserRepository.UserRecord;
import com.plasmit.auth.hospital.security.SecurityConfig.CurrentUser;

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
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(UserRecord user) {
        return generateToken(user, accessTokenExpirationMs, "ACCESS");
    }

    public String generateRefreshToken(UserRecord user) {
        return generateToken(user, refreshTokenExpirationMs, "REFRESH");
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

        String name = claims.get("name", String.class);
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);

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
                name,
                email,
                role,
                permissions
        );
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    private String generateToken(UserRecord user, long expirationMs, String tokenType) {

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        log.info("Generating token. tokenType={} userId={} tenantId={} hospitalId={}",
                tokenType, user.id(), user.tenantId(), user.hospitalId());

        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("tokenType", tokenType)
                .claim("userId", user.id())
                .claim("tenantId", user.tenantId())
                .claim("hospitalId", user.hospitalId())
                .claim("branchId", user.branchId())
                .claim("departmentId", user.departmentId())
                .claim("name", user.fullName())
                .claim("email", user.email())
                .claim("role", user.roleCode())
                .claim("permissions", user.permissions())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
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