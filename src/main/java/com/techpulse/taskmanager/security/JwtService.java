package com.techpulse.taskmanager.security;

import com.techpulse.taskmanager.user.Role;
import com.techpulse.taskmanager.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies HS256-signed JWTs.
 * <p>
 * Custom claims: {@code tenant_id}, {@code role}, {@code email}. The subject is the user id.
 */
@Service
public class JwtService {

    public static final String CLAIM_TENANT_ID = "tenant_id";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EMAIL = "email";
    public static final String ISSUER = "multi-tenant-task-manager";

    private final SecretKey signingKey;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
        this.clock = clock;
    }

    public String generateToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_TENANT_ID, user.getTenantId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies signature, issuer and expiry, then maps the claims to a principal.
     *
     * @throws JwtException if the token is invalid, expired or missing required claims
     */
    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        try {
            Long userId = Long.valueOf(claims.getSubject());
            Long tenantId = ((Number) claims.get(CLAIM_TENANT_ID)).longValue();
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            String email = claims.get(CLAIM_EMAIL, String.class);
            return new AuthenticatedUser(userId, tenantId, email, role);
        } catch (RuntimeException e) {
            throw new MalformedJwtException("Token is missing required claims", e);
        }
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }
}
