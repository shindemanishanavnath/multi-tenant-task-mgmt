package com.techpulse.taskmanager.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.techpulse.taskmanager.security.AuthenticatedUser;
import com.techpulse.taskmanager.security.JwtService;
import com.techpulse.taskmanager.support.ApiTestSupport;
import com.techpulse.taskmanager.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /auth/login")
class AuthApiTest extends ApiTestSupport {

    @Autowired
    private JwtService jwtService;

    @Test
    void returnsJwtWithTenantIdAndRoleClaims() throws Exception {
        TenantFixture tenant = registerTenant();

        String body = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", tenant.adminEmail(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andExpect(jsonPath("$.tenantId").value(tenant.tenantId()))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        String token = read(body).get("accessToken").asText();

        // raw payload carries the custom claims
        JsonNode payload = read(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8));
        assertThat(payload.get("tenant_id").asLong()).isEqualTo(tenant.tenantId());
        assertThat(payload.get("role").asText()).isEqualTo("ADMIN");
        assertThat(payload.get("sub").asText()).isEqualTo(String.valueOf(tenant.adminUserId()));
        assertThat(payload.has("exp")).isTrue();

        AuthenticatedUser parsed = jwtService.parseToken(token);
        assertThat(parsed.tenantId()).isEqualTo(tenant.tenantId());
        assertThat(parsed.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void emailIsCaseInsensitive() throws Exception {
        TenantFixture tenant = registerTenant();
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", tenant.adminEmail().toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        TenantFixture tenant = registerTenant();
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", tenant.adminEmail(), "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void rejectsUnknownEmailWithSameMessage() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", unique("ghost") + "@example.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void validatesRequestBody() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "", "password", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void ignoresStaleTokenOnPublicEndpoint() throws Exception {
        TenantFixture tenant = registerTenant();
        mockMvc.perform(withToken(post("/auth/login"), "garbage.token.value").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", tenant.adminEmail(), "password", PASSWORD))))
                .andExpect(status().isOk());
    }
}
