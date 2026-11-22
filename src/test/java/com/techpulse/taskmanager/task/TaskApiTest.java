package com.techpulse.taskmanager.task;

import com.techpulse.taskmanager.security.JwtProperties;
import com.techpulse.taskmanager.security.JwtService;
import com.techpulse.taskmanager.support.ApiTestSupport;
import com.techpulse.taskmanager.user.Role;
import com.techpulse.taskmanager.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("/tasks")
class TaskApiTest extends ApiTestSupport {

    @Autowired
    private JwtProperties jwtProperties;

    private TenantFixture tenantA;
    private UserFixture managerA;
    private UserFixture userA;
    private UserFixture otherUserA;

    private TenantFixture tenantB;
    private UserFixture managerB;
    private UserFixture userB;

    @BeforeEach
    void setUpTwoTenants() throws Exception {
        tenantA = registerTenant();
        managerA = createUser(tenantA, "MANAGER");
        userA = createUser(tenantA, "USER");
        otherUserA = createUser(tenantA, "USER");

        tenantB = registerTenant();
        managerB = createUser(tenantB, "MANAGER");
        userB = createUser(tenantB, "USER");
    }

    @Nested
    @DisplayName("POST /tasks")
    class CreateTask {

        @Test
        void managerCreatesTaskInOwnTenant() throws Exception {
            mockMvc.perform(withToken(post("/tasks"), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("title", "Write README", "description", "docs"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Write README"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.tenantId").value(tenantA.tenantId()))
                    .andExpect(jsonPath("$.assignedTo").doesNotExist());
        }

        @Test
        void managerCreatesTaskWithAssignee() throws Exception {
            mockMvc.perform(withToken(post("/tasks"), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("title", "Assigned", "assignedTo", userA.id()))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.assignedTo").value(userA.id()))
                    .andExpect(jsonPath("$.assignedToEmail").value(userA.email()));
        }

        @Test
        void cannotCreateTaskForUserOfAnotherTenant() throws Exception {
            mockMvc.perform(withToken(post("/tasks"), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("title", "Sneaky", "assignedTo", userB.id()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        void tenantIdInBodyIsRejected() throws Exception {
            mockMvc.perform(withToken(post("/tasks"), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("title", "Spoof", "tenant_id", tenantB.tenantId()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("taken from the JWT")));
        }

        @Test
        void adminCannotCreateTasks() throws Exception {
            mockMvc.perform(withToken(post("/tasks"), tenantA.adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("title", "Nope"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void userCannotCreateTasks() throws Exception {
            mockMvc.perform(withToken(post("/tasks"), userA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("title", "Nope"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void titleIsRequired() throws Exception {
            mockMvc.perform(withToken(post("/tasks"), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("description", "no title"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.title").exists());
        }
    }

    @Nested
    @DisplayName("PUT /tasks/{id}/assign")
    class AssignTask {

        @Test
        void managerAssignsTaskToUserInSameTenant() throws Exception {
            long taskId = createTask(managerA, null);

            mockMvc.perform(withToken(put("/tasks/{id}/assign", taskId), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("userId", userA.id()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignedTo").value(userA.id()));
        }

        @Test
        void managerCanReassign() throws Exception {
            long taskId = createTask(managerA, userA.id());

            mockMvc.perform(withToken(put("/tasks/{id}/assign", taskId), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("userId", otherUserA.id()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assignedTo").value(otherUserA.id()));
        }

        @Test
        void cannotAssignToUserOfAnotherTenant() throws Exception {
            long taskId = createTask(managerA, null);

            mockMvc.perform(withToken(put("/tasks/{id}/assign", taskId), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("userId", userB.id()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        void cannotAssignTaskOfAnotherTenant() throws Exception {
            long taskOfA = createTask(managerA, null);

            mockMvc.perform(withToken(put("/tasks/{id}/assign", taskOfA), managerB.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("userId", userB.id()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Task not found"));
        }

        @Test
        void cannotAssignToNonUserRole() throws Exception {
            long taskId = createTask(managerA, null);

            mockMvc.perform(withToken(put("/tasks/{id}/assign", taskId), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("userId", managerA.id()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("role USER")));
        }

        @Test
        void unknownTaskIsNotFound() throws Exception {
            mockMvc.perform(withToken(put("/tasks/{id}/assign", 999_999), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("userId", userA.id()))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void userIdIsRequired() throws Exception {
            long taskId = createTask(managerA, null);
            mockMvc.perform(withToken(put("/tasks/{id}/assign", taskId), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.userId").exists());
        }

        @Test
        void nonNumericIdIsBadRequest() throws Exception {
            mockMvc.perform(withToken(put("/tasks/{id}/assign", "abc"), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("userId", userA.id()))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for 'id'"));
        }

        @Test
        void userCannotAssign() throws Exception {
            long taskId = createTask(managerA, null);
            mockMvc.perform(withToken(put("/tasks/{id}/assign", taskId), userA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("userId", userA.id()))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /tasks/{id}/status")
    class UpdateStatus {

        @Test
        void assigneeUpdatesStatus() throws Exception {
            long taskId = createTask(managerA, userA.id());

            mockMvc.perform(withToken(put("/tasks/{id}/status", taskId), userA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("status", "IN_PROGRESS"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void userCannotUpdateTaskAssignedToSomeoneElse() throws Exception {
            long taskId = createTask(managerA, otherUserA.id());

            mockMvc.perform(withToken(put("/tasks/{id}/status", taskId), userA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("status", "DONE"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void userCannotUpdateTaskOfAnotherTenant() throws Exception {
            long taskOfA = createTask(managerA, userA.id());

            mockMvc.perform(withToken(put("/tasks/{id}/status", taskOfA), userB.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("status", "DONE"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Task not found"));
        }

        @Test
        void managerCannotUpdateStatus() throws Exception {
            long taskId = createTask(managerA, userA.id());

            mockMvc.perform(withToken(put("/tasks/{id}/status", taskId), managerA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("status", "DONE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void invalidStatusIsBadRequest() throws Exception {
            long taskId = createTask(managerA, userA.id());

            mockMvc.perform(withToken(put("/tasks/{id}/status", taskId), userA.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("status", "FINISHED"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("TODO, IN_PROGRESS, DONE")));
        }
    }

    @Nested
    @DisplayName("GET /tasks")
    class ListTasks {

        @Test
        void adminSeesAllTasksOfOwnTenantOnly() throws Exception {
            long t1 = createTask(managerA, userA.id());
            long t2 = createTask(managerA, null);
            long taskOfB = createTask(managerB, userB.id());

            mockMvc.perform(withToken(get("/tasks"), tenantA.adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", hasItem((int) t1)))
                    .andExpect(jsonPath("$[*].id", hasItem((int) t2)))
                    .andExpect(jsonPath("$[*].id", not(hasItem((int) taskOfB))));
        }

        @Test
        void managerSeesAllTasksOfOwnTenantOnly() throws Exception {
            createTask(managerA, userA.id());
            long taskOfB = createTask(managerB, null);

            mockMvc.perform(withToken(get("/tasks"), managerB.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(taskOfB))
                    .andExpect(jsonPath("$[0].tenantId").value(tenantB.tenantId()));
        }

        @Test
        void userSeesOnlyAssignedTasks() throws Exception {
            long mine = createTask(managerA, userA.id());
            createTask(managerA, otherUserA.id());
            createTask(managerA, null);

            mockMvc.perform(withToken(get("/tasks"), userA.token()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(mine));
        }

        @Test
        void tenantIdQueryParamIsRejected() throws Exception {
            mockMvc.perform(withToken(get("/tasks"), managerA.token())
                            .param("tenantId", String.valueOf(tenantB.tenantId())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("JWT validation")
    class JwtValidation {

        @Test
        void missingTokenIsUnauthorized() throws Exception {
            mockMvc.perform(get("/tasks"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication required"));
        }

        @Test
        void nonBearerHeaderIsUnauthorized() throws Exception {
            mockMvc.perform(get("/tasks").header("Authorization", "Basic abc"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void garbageTokenIsUnauthorized() throws Exception {
            mockMvc.perform(withToken(get("/tasks"), "not-a-jwt"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }

        @Test
        void tamperedTokenIsUnauthorized() throws Exception {
            // swap the payload for one claiming tenant B, keep tenant A's signature
            String[] partsA = managerA.token().split("\\.");
            String[] partsB = managerB.token().split("\\.");
            String forged = partsA[0] + "." + partsB[1] + "." + partsA[2];

            mockMvc.perform(withToken(get("/tasks"), forged))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void expiredTokenIsUnauthorized() throws Exception {
            Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
            JwtService pastIssuer = new JwtService(jwtProperties, past);
            User manager = new User(managerA.email(), "x", Role.MANAGER, tenantA.tenantId());
            ReflectionTestUtils.setField(manager, "id", managerA.id());

            mockMvc.perform(withToken(get("/tasks"), pastIssuer.generateToken(manager)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }

        @Test
        void unknownRouteNeedsAuthThenReturns404() throws Exception {
            mockMvc.perform(withToken(get("/nope"), managerA.token()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void unsupportedMethodIs405() throws Exception {
            mockMvc.perform(withToken(delete("/tasks"), managerA.token()))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
