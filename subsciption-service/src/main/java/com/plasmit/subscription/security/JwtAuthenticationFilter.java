package com.plasmit.subscription.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);

            if (!jwtService.isTokenValid(token)) {
                log.warn("JWT validation failed. uri={}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtService.extractUserId(token);
            Long tenantId = jwtService.extractTenantId(token);
            String role = jwtService.extractRole(token);
            String userType = jwtService.extractUserType(token);

            TenantContext.setUserId(userId);
            TenantContext.setTenantId(tenantId);
            TenantContext.setRole(role);
            TenantContext.setUserType(userType);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT verified. userId={}, tenantId={}, role={}", userId, tenantId, role);

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}