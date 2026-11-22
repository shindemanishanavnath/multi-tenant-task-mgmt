package com.techpulse.taskmanager.task;

import com.techpulse.taskmanager.common.ApiException;
import com.techpulse.taskmanager.security.AuthenticatedUser;
import com.techpulse.taskmanager.security.TenantContext;
import com.techpulse.taskmanager.task.TaskDtos.CreateTaskRequest;
import com.techpulse.taskmanager.task.TaskDtos.TaskResponse;
import com.techpulse.taskmanager.user.Role;
import com.techpulse.taskmanager.user.User;
import com.techpulse.taskmanager.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * All task operations read the tenant from {@link TenantContext} (populated from the JWT) and
 * only use tenant-scoped repository methods. A task or user from another tenant is reported as
 * "not found" (404), so callers cannot even confirm that another tenant's ids exist.
 */
@Service
public class TaskService {

    static final String TASK_NOT_FOUND = "Task not found";
    static final String USER_NOT_FOUND = "User not found";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Task task = new Task(request.title().trim(), request.description(), tenantId);
        if (request.assignedTo() != null) {
            task.assignTo(findAssignableUser(request.assignedTo(), tenantId));
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse assignTask(Long taskId, Long userId) {
        Long tenantId = TenantContext.requireTenantId();
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> ApiException.notFound(TASK_NOT_FOUND));
        task.assignTo(findAssignableUser(userId, tenantId));
        return TaskResponse.from(taskRepository.save(task));
    }

    /** A USER may only change the status of a task that is in their tenant and assigned to them. */
    @Transactional
    public TaskResponse updateStatus(Long taskId, TaskStatus status, AuthenticatedUser caller) {
        Long tenantId = TenantContext.requireTenantId();
        Task task = taskRepository.findByIdAndTenantIdAndAssignedToId(taskId, tenantId, caller.userId())
                .orElseThrow(() -> ApiException.notFound(TASK_NOT_FOUND));
        task.setStatus(status);
        return TaskResponse.from(taskRepository.save(task));
    }

    /** ADMIN and MANAGER see every task of their tenant; USER sees only tasks assigned to them. */
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(AuthenticatedUser caller) {
        Long tenantId = TenantContext.requireTenantId();
        List<Task> tasks = caller.role() == Role.USER
                ? taskRepository.findAllByTenantIdAndAssignedToIdOrderByIdAsc(tenantId, caller.userId())
                : taskRepository.findAllByTenantIdOrderByIdAsc(tenantId);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    private User findAssignableUser(Long userId, Long tenantId) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> ApiException.notFound(USER_NOT_FOUND));
        if (user.getRole() != Role.USER) {
            throw ApiException.badRequest("Tasks can only be assigned to users with role USER");
        }
        return user;
    }
}
