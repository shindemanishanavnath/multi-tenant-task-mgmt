package com.techpulse.taskmanager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techpulse.taskmanager.common.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Rejects any request that tries to pass a tenant id as a query/form parameter.
 * The tenant is resolved only from the JWT, so a client-supplied value is always a mistake
 * or an attack; it is refused loudly (400) rather than silently ignored.
 */
public class TenantParamGuardFilter extends OncePerRequestFilter {

    static final Set<String> FORBIDDEN_PARAMS = Set.of("tenant_id", "tenantid");
    public static final String MESSAGE = "tenant_id must not be supplied by the client; it is taken from the JWT";

    private final ObjectMapper objectMapper;

    public TenantParamGuardFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static boolean isTenantField(String name) {
        return name != null && FORBIDDEN_PARAMS.contains(name.toLowerCase());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean smuggled = request.getParameterMap().keySet().stream()
                .anyMatch(TenantParamGuardFilter::isTenantField);
        if (smuggled) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    ErrorResponse.of(HttpStatus.BAD_REQUEST, MESSAGE, request.getRequestURI()));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
