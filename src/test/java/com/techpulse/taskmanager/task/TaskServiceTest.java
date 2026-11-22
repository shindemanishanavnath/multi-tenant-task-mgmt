package com.techpulse.taskmanager.task;

import com.techpulse.taskmanager.common.ApiException;
import com.techpulse.taskmanager.security.AuthenticatedUser;
import com.techpulse.taskmanager.security.TenantContext;
import com.techpulse.taskmanager.task.TaskDtos.CreateTaskRequest;
import com.techpulse.taskmanager.task.TaskDtos.TaskResponse;
import com.techpulse.taskmanager.user.Role;
import com.techpulse.taskmanager.user.User;
import com.techpulse.taskmanager.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests: verify the service always passes the JWT tenant to the repository. */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final long TENANT = 10L;

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void bindTenant() {
        TenantContext.setTenantId(TENANT);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static User user(long id, long tenantId, Role role) {
        User user = new User("u" + id + "@x.com", "hash", role, tenantId);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void createUsesTenantFromContext() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.createTask(new CreateTaskRequest("  Title  ", "d", null));

        assertThat(response.tenantId()).isEqualTo(TENANT);
        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void createLooksUpAssigneeWithinTenantOnly() {
        when(userRepository.findByIdAndTenantId(5L, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(new CreateTaskRequest("t", null, 5L)))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void assignRejectsTaskOutsideTenant() {
        when(taskRepository.findByIdAndTenantId(1L, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assignTask(1L, 2L))
                .isInstanceOf(ApiException.class)
                .hasMessage(TaskService.TASK_NOT_FOUND);
        verify(userRepository, never()).findByIdAndTenantId(anyLong(), anyLong());
    }

    @Test
    void assignRejectsNonUserRole() {
        when(taskRepository.findByIdAndTenantId(1L, TENANT)).thenReturn(Optional.of(new Task("t", null, TENANT)));
        when(userRepository.findByIdAndTenantId(2L, TENANT)).thenReturn(Optional.of(user(2, TENANT, Role.ADMIN)));

        assertThatThrownBy(() -> taskService.assignTask(1L, 2L))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void entityRefusesAssigneeFromAnotherTenant() {
        Task task = new Task("t", null, TENANT);
        assertThatThrownBy(() -> task.assignTo(user(3, 99, Role.USER)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void statusUpdateChecksOwnership() {
        AuthenticatedUser caller = new AuthenticatedUser(4L, TENANT, "u@x.com", Role.USER);
        when(taskRepository.findByIdAndTenantIdAndAssignedToId(1L, TENANT, 4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(1L, TaskStatus.DONE, caller))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listForUserReturnsOnlyAssigned() {
        AuthenticatedUser caller = new AuthenticatedUser(4L, TENANT, "u@x.com", Role.USER);
        when(taskRepository.findAllByTenantIdAndAssignedToIdOrderByIdAsc(TENANT, 4L)).thenReturn(List.of());

        assertThat(taskService.listTasks(caller)).isEmpty();
        verify(taskRepository, never()).findAllByTenantIdOrderByIdAsc(anyLong());
    }

    @Test
    void listForAdminReturnsWholeTenant() {
        AuthenticatedUser caller = new AuthenticatedUser(1L, TENANT, "a@x.com", Role.ADMIN);
        when(taskRepository.findAllByTenantIdOrderByIdAsc(TENANT)).thenReturn(List.of(new Task("t", null, TENANT)));

        assertThat(taskService.listTasks(caller)).hasSize(1);
    }

    @Test
    void failsWithoutTenantContext() {
        TenantContext.clear();
        assertThatThrownBy(() -> taskService.createTask(new CreateTaskRequest("t", null, null)))
                .isInstanceOf(IllegalStateException.class);
    }
}
