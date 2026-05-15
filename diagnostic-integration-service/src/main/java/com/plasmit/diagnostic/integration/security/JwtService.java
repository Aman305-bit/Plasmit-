package com.plasmit.diagnostic.integration.security;

import com.plasmit.diagnostic.integration.common.exception.ApiException;
import com.plasmit.diagnostic.integration.context.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public CurrentUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = parseLong(claims.getSubject());
            Long tenantId = parseLong(claims.get("tenantId"));
            Long hospitalId = parseLong(claims.get("hospitalId"));

            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);
            String userType = claims.get("userType", String.class);

            if (userId == null) {
                throw ApiException.unauthorized("User id missing from token.");
            }

            if (tenantId == null) {
                throw ApiException.unauthorized("Tenant id missing from token.");
            }

            return new CurrentUser(
                    userId,
                    tenantId,
                    hospitalId,
                    email,
                    role,
                    userType
            );

        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ApiException.unauthorized("Invalid or expired token.");
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value);

        if (text.isBlank()) {
            return null;
        }

        return Long.valueOf(text);
    }
}