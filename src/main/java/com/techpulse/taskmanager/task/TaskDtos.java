package com.techpulse.taskmanager.task;

import com.techpulse.taskmanager.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class TaskDtos {

    private TaskDtos() {
    }

    /** No tenant field: the task is always created in the caller's tenant (from the JWT). */
    public record CreateTaskRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            Long assignedTo) {
    }

    public record AssignTaskRequest(@NotNull Long userId) {
    }

    public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {
    }

    public record TaskResponse(Long id,
                               String title,
                               String description,
                               TaskStatus status,
                               Long assignedTo,
                               String assignedToEmail,
                               Long tenantId,
                               Instant createdAt) {

        public static TaskResponse from(Task task) {
            User assignee = task.getAssignedTo();
            return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                    assignee != null ? assignee.getId() : null,
                    assignee != null ? assignee.getEmail() : null,
                    task.getTenantId(), task.getCreatedAt());
        }
    }
}
