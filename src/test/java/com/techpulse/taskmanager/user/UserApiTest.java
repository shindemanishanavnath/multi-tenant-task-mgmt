package com.techpulse.taskmanager.user;

import com.techpulse.taskmanager.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("/users")
class UserApiTest extends ApiTestSupport {

    @Test
    void adminCreatesUserInOwnTenant() throws Exception {
        TenantFixture tenant = registerTenant();
        String email = unique("new") + "@example.com";

        mockMvc.perform(withToken(post("/users"), tenant.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD, "role", "MANAGER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.tenantId").value(tenant.tenantId()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createdUserTokenCarriesAdminsTenant() throws Exception {
        TenantFixture tenant = registerTenant();
        UserFixture user = createUser(tenant, "USER");

        mockMvc.perform(withToken(get("/tasks"), user.token()))
                .andExpect(status().isOk());
        String body = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.email(), "password", PASSWORD))))
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(read(body).get("tenantId").asLong()).isEqualTo(tenant.tenantId());
    }

    @Test
    void managerCannotCreateUsers() throws Exception {
        TenantFixture tenant = registerTenant();
        UserFixture manager = createUser(tenant, "MANAGER");

        mockMvc.perform(withToken(post("/users"), manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", unique("x") + "@example.com", "password", PASSWORD, "role", "USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotCreateUsers() throws Exception {
        TenantFixture tenant = registerTenant();
        UserFixture user = createUser(tenant, "USER");

        mockMvc.perform(withToken(post("/users"), user.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", unique("x") + "@example.com", "password", PASSWORD, "role", "USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", unique("x") + "@example.com", "password", PASSWORD, "role", "USER"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void rejectsDuplicateEmailEvenAcrossTenants() throws Exception {
        TenantFixture tenantA = registerTenant();
        TenantFixture tenantB = registerTenant();
        UserFixture existing = createUser(tenantA, "USER");

        mockMvc.perform(withToken(post("/users"), tenantB.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", existing.email(), "password", PASSWORD, "role", "USER"))))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsTenantIdInBody() throws Exception {
        TenantFixture tenantA = registerTenant();
        TenantFixture tenantB = registerTenant();

        mockMvc.perform(withToken(post("/users"), tenantA.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", unique("x") + "@example.com", "password", PASSWORD,
                                "role", "USER", "tenantId", tenantB.tenantId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("taken from the JWT")));
    }

    @Test
    void rejectsTenantIdQueryParameter() throws Exception {
        TenantFixture tenantA = registerTenant();
        TenantFixture tenantB = registerTenant();

        mockMvc.perform(withToken(post("/users"), tenantA.adminToken())
                        .param("tenant_id", String.valueOf(tenantB.tenantId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", unique("x") + "@example.com", "password", PASSWORD, "role", "USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("taken from the JWT")));
    }

    @Test
    void rejectsInvalidRole() throws Exception {
        TenantFixture tenant = registerTenant();

        mockMvc.perform(withToken(post("/users"), tenant.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", unique("x") + "@example.com", "password", PASSWORD, "role", "SUPERUSER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Allowed values: ADMIN, MANAGER, USER")));
    }

    @Test
    void validatesRequestBody() throws Exception {
        TenantFixture tenant = registerTenant();

        mockMvc.perform(withToken(post("/users"), tenant.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.password", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.role", notNullValue()));
    }

    @Test
    void listReturnsOnlyOwnTenantUsers() throws Exception {
        TenantFixture tenantA = registerTenant();
        TenantFixture tenantB = registerTenant();
        UserFixture managerA = createUser(tenantA, "MANAGER");
        UserFixture userA = createUser(tenantA, "USER");
        UserFixture userB = createUser(tenantB, "USER");

        mockMvc.perform(withToken(get("/users"), managerA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].email", hasItem(userA.email())))
                .andExpect(jsonPath("$[*].email", not(hasItem(userB.email()))))
                .andExpect(jsonPath("$[*].tenantId", everyItem(is((int) tenantA.tenantId()))));
    }

    @Test
    void userCannotListUsers() throws Exception {
        TenantFixture tenant = registerTenant();
        UserFixture user = createUser(tenant, "USER");

        mockMvc.perform(withToken(get("/users"), user.token()))
                .andExpect(status().isForbidden());
    }
}
