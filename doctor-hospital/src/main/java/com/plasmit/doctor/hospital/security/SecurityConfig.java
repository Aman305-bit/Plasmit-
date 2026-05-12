package com.plasmit.doctor.hospital.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.plasmit.doctor.hospital.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        RequestContextFilter requestContextFilter = new RequestContextFilter();
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtService);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(requestContextFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthFilter, RequestContextFilter.class);

        return http.build();
    }

    public static class CurrentUser {

        private final Long userId;
        private final Long tenantId;
        private final Long hospitalId;
        private final Long branchId;
        private final Long departmentId;
        private final String name;
        private final String email;
        private final String role;
        private final List<String> permissions;

        public CurrentUser(
                Long userId,
                Long tenantId,
                Long hospitalId,
                Long branchId,
                Long departmentId,
                String name,
                String email,
                String role,
                List<String> permissions
        ) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.hospitalId = hospitalId;
            this.branchId = branchId;
            this.departmentId = departmentId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.permissions = permissions;
        }

        public boolean hasPermission(String permission) {
            return permissions != null && permissions.contains(permission);
        }

        public Long getUserId() {
            return userId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public Long getHospitalId() {
            return hospitalId;
        }

        public Long getBranchId() {
            return branchId;
        }

        public Long getDepartmentId() {
            return departmentId;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }

        public List<String> getPermissions() {
            return permissions;
        }
    }

    public static class TenantContext {

        private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

        private TenantContext() {
        }

        public static void set(CurrentUser currentUser) {
            CURRENT_USER.set(currentUser);
        }

        public static CurrentUser get() {
            return CURRENT_USER.get();
        }

        public static void clear() {
            CURRENT_USER.remove();
        }
    }

    public static class RequestContextFilter extends OncePerRequestFilter {

        private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {

            String requestId = request.getHeader("X-Request-Id");

            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }

            MDC.put("requestId", requestId);
            response.setHeader("X-Request-Id", requestId);

            long startTime = System.currentTimeMillis();

            try {
                log.info("Request received. method={} path={}", request.getMethod(), request.getRequestURI());
                filterChain.doFilter(request, response);
            } finally {
                long durationMs = System.currentTimeMillis() - startTime;

                log.info("Request completed. method={} path={} status={} durationMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        durationMs);

                MDC.clear();
                TenantContext.clear();
                SecurityContextHolder.clearContext();
            }
        }
    }

    public static class JwtAuthFilter extends OncePerRequestFilter {

        private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

        private final JwtService jwtService;

        public JwtAuthFilter(JwtService jwtService) {
            this.jwtService = jwtService;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {

            String authorization = request.getHeader("Authorization");

            if (authorization == null || !authorization.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authorization.substring(7);

            try {
                CurrentUser currentUser = jwtService.validateAccessToken(token);

                TenantContext.set(currentUser);

                MDC.put("tenantId", String.valueOf(currentUser.getTenantId()));
                MDC.put("hospitalId", String.valueOf(currentUser.getHospitalId()));
                MDC.put("branchId", String.valueOf(currentUser.getBranchId()));
                MDC.put("userId", String.valueOf(currentUser.getUserId()));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                currentUser,
                                null,
                                currentUser.getPermissions()
                                        .stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .toList()
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("JWT verified. userId={} tenantId={} hospitalId={} branchId={}",
                        currentUser.getUserId(),
                        currentUser.getTenantId(),
                        currentUser.getHospitalId(),
                        currentUser.getBranchId());

            } catch (Exception ex) {
                log.warn("JWT verification failed. error={}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }

            filterChain.doFilter(request, response);
        }
    }
}