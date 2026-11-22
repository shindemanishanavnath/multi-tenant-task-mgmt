package com.techpulse.taskmanager.tenant;

import com.techpulse.taskmanager.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /tenant/register")
class TenantApiTest extends ApiTestSupport {

    @Test
    void registersTenantAndFirstAdmin() throws Exception {
        String name = unique("acme");
        String email = unique("owner") + "@ACME.com";

        mockMvc.perform(post("/tenant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tenantName", name, "adminEmail", email, "adminPassword", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId", notNullValue()))
                .andExpect(jsonPath("$.tenantName").value(name))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.adminUserId", notNullValue()))
                .andExpect(jsonPath("$.adminEmail").value(email.toLowerCase()));

        // the first admin can log in straight away
        login(email.toLowerCase());
    }

    @Test
    void rejectsDuplicateTenantName() throws Exception {
        String name = unique("dup");
        mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tenantName", name, "adminEmail", unique("a") + "@x.com", "adminPassword", PASSWORD))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tenantName", name.toUpperCase(), "adminEmail", unique("b") + "@x.com", "adminPassword", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("tenant")));
    }

    @Test
    void rejectsDuplicateAdminEmail() throws Exception {
        TenantFixture existing = registerTenant();

        mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tenantName", unique("other"), "adminEmail", existing.adminEmail(), "adminPassword", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    void validatesRequestBody() throws Exception {
        mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tenantName", "", "adminEmail", "not-an-email", "adminPassword", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.tenantName", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.adminEmail", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.adminPassword", notNullValue()));
    }

    @Test
    void rejectsClientSuppliedTenantIdInBody() throws Exception {
        mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tenantName", unique("t"), "adminEmail", unique("a") + "@x.com",
                                "adminPassword", PASSWORD, "tenant_id", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("taken from the JWT")));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void rejectsUnknownField() throws Exception {
        mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tenantName", unique("t"), "adminEmail", unique("a") + "@x.com",
                                "adminPassword", PASSWORD, "plan", "gold"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown field 'plan'"));
    }
}
