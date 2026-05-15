package com.plasmit.diagnostics.payment.security;

import com.plasmit.diagnostics.payment.common.constant.HeaderConstants;
import com.plasmit.diagnostics.payment.context.CurrentUser;
import com.plasmit.diagnostics.payment.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestId = request.getHeader(HeaderConstants.X_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String branchId = request.getHeader(HeaderConstants.X_BRANCH_ID);

        try {
            TenantContext.setRequestId(requestId);
            TenantContext.setBranchId(branchId);

            MDC.put("requestId", requestId);
            MDC.put("branchId", branchId);

            String authHeader = request.getHeader(HeaderConstants.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith(HeaderConstants.BEARER_PREFIX)) {

                String token = authHeader.substring(HeaderConstants.BEARER_PREFIX.length());
                CurrentUser currentUser = jwtService.parseToken(token);

                TenantContext.setCurrentUser(currentUser);

                MDC.put("tenantId", String.valueOf(currentUser.getTenantId()));
                MDC.put("hospitalId", String.valueOf(currentUser.getHospitalId()));
                MDC.put("userId", String.valueOf(currentUser.getUserId()));

                String role = currentUser.getRole() == null ? "USER" : currentUser.getRole();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                currentUser.getEmail(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
            MDC.clear();
            SecurityContextHolder.clearContext();
        }
    }
}