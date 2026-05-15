package com.plasmit.diagnostic.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plasmit.diagnostic.integration.common.constant.HeaderConstants;
import com.plasmit.diagnostic.integration.common.exception.ErrorCode;
import com.plasmit.diagnostic.integration.common.response.ApiMeta;
import com.plasmit.diagnostic.integration.common.response.ApiResponse;
import com.plasmit.diagnostic.integration.context.CurrentUser;
import com.plasmit.diagnostic.integration.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
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

            if (branchId != null) {
                MDC.put("branchId", branchId);
            }

            String authHeader = request.getHeader(HeaderConstants.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith(HeaderConstants.BEARER_PREFIX)) {
                writeUnauthorizedResponse(response, requestId, "Authorization Bearer token is required.");
                return;
            }

            String token = authHeader.substring(HeaderConstants.BEARER_PREFIX.length());
            CurrentUser user = jwtService.parseToken(token);

            TenantContext.setCurrentUser(user);

            MDC.put("tenantId", String.valueOf(user.getTenantId()));

            if (user.getHospitalId() != null) {
                MDC.put("hospitalId", String.valueOf(user.getHospitalId()));
            }

            MDC.put("userId", String.valueOf(user.getUserId()));

            String role = user.getRole() == null || user.getRole().isBlank()
                    ? "USER"
                    : user.getRole();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            writeUnauthorizedResponse(response, requestId, ex.getMessage());
        } finally {
            TenantContext.clear();
            MDC.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void writeUnauthorizedResponse(HttpServletResponse response,
                                           String requestId,
                                           String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", ErrorCode.UNAUTHORIZED.name());
        error.put("message", message == null || message.isBlank()
                ? "Unauthorized request."
                : message);

        ApiResponse<Map<String, Object>> apiResponse = new ApiResponse<>(
                false,
                error.get("message").toString(),
                error,
                new ApiMeta(requestId)
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}