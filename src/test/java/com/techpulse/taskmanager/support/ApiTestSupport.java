package com.techpulse.taskmanager.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared helpers for API tests: every test builds its own tenants/users with unique names,
 * so tests are independent while sharing one Spring context and one in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "Password@123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** A registered tenant plus its admin's token. */
    protected record TenantFixture(long tenantId, String adminEmail, String adminToken, long adminUserId) {
    }

    /** A logged-in user. */
    protected record UserFixture(long id, String email, String token) {
    }

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected JsonNode read(String content) throws Exception {
        return objectMapper.readTree(content);
    }

    protected MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected TenantFixture registerTenant() throws Exception {
        String name = unique("tenant");
        String adminEmail = unique("admin") + "@example.com";
        String body = mockMvc.perform(post("/tenant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tenantName", name, "adminEmail", adminEmail, "adminPassword", PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = read(body);
        return new TenantFixture(node.get("tenantId").asLong(), adminEmail, login(adminEmail),
                node.get("adminUserId").asLong());
    }

    protected String login(String email) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return read(body).get("accessToken").asText();
    }

    protected UserFixture createUser(TenantFixture tenant, String role) throws Exception {
        String email = unique(role.toLowerCase()) + "@example.com";
        String body = mockMvc.perform(withToken(post("/users"), tenant.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD, "role", role))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new UserFixture(read(body).get("id").asLong(), email, login(email));
    }

    protected long createTask(UserFixture manager, Long assigneeId) throws Exception {
        Map<String, Object> request = assigneeId == null
                ? Map.of("title", unique("task"), "description", "desc")
                : Map.of("title", unique("task"), "description", "desc", "assignedTo", assigneeId);
        String body = mockMvc.perform(withToken(post("/tasks"), manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return read(body).get("id").asLong();
    }
}
