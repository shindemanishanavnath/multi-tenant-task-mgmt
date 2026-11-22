package com.techpulse.taskmanager.security;

import com.techpulse.taskmanager.user.Role;
import com.techpulse.taskmanager.user.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-bytes-long";
    private final JwtProperties properties = new JwtProperties(SECRET, 60);
    private final JwtService jwtService = new JwtService(properties, Clock.systemUTC());

    private static User user(long id, long tenantId, Role role) {
        User user = new User("someone@example.com", "hash", role, tenantId);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void roundTripKeepsTenantAndRole() {
        String token = jwtService.generateToken(user(7, 42, Role.MANAGER));

        AuthenticatedUser parsed = jwtService.parseToken(token);

        assertThat(parsed.userId()).isEqualTo(7L);
        assertThat(parsed.tenantId()).isEqualTo(42L);
        assertThat(parsed.role()).isEqualTo(Role.MANAGER);
        assertThat(parsed.email()).isEqualTo("someone@example.com");
        assertThat(parsed.authorities()).extracting(Object::toString).containsExactly("ROLE_MANAGER");
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600);
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtService other = new JwtService(new JwtProperties("a-completely-different-secret-of-32-bytes!!", 60), Clock.systemUTC());
        String token = other.generateToken(user(1, 1, Role.ADMIN));

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(3)), ZoneOffset.UTC);
        String token = new JwtService(properties, past).generateToken(user(1, 1, Role.USER));

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsTokenWithoutTenantClaim() {
        String token = Jwts.builder()
                .issuer(JwtService.ISSUER)
                .subject("1")
                .claim(JwtService.CLAIM_ROLE, "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void rejectsTokenWithWrongIssuer() {
        String token = Jwts.builder()
                .issuer("someone-else")
                .subject("1")
                .claim(JwtService.CLAIM_TENANT_ID, 1)
                .claim(JwtService.CLAIM_ROLE, "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void propertiesRejectShortSecretAndBadExpiry() {
        assertThatThrownBy(() -> new JwtProperties("short", 60)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(null, 60)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(SECRET, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
