package com.techpulse.taskmanager.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request:
 * <ol>
 *     <li>reads the {@code Authorization: Bearer <jwt>} header,</li>
 *     <li>verifies the token via {@link JwtService},</li>
 *     <li>puts the principal (with tenant id and role) into the {@link SecurityContext},</li>
 *     <li>binds the tenant id to {@link TenantContext} for the rest of the request,</li>
 *     <li>clears both when the request finishes.</li>
 * </ol>
 * An invalid token leaves the request unauthenticated; Spring Security then answers 401 for any
 * protected endpoint (the reason is passed to the entry point via a request attribute).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String JWT_ERROR_ATTRIBUTE = "jwt_error";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                authenticate(header.substring(BEARER_PREFIX.length()).trim(), request);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            AuthenticatedUser user = jwtService.parseToken(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.authorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            TenantContext.setTenantId(user.tenantId());
        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute(JWT_ERROR_ATTRIBUTE, "Invalid or expired token");
        }
    }
}
