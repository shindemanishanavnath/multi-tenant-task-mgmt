package com.techpulse.taskmanager.task;

import com.techpulse.taskmanager.security.AuthenticatedUser;
import com.techpulse.taskmanager.task.TaskDtos.AssignTaskRequest;
import com.techpulse.taskmanager.task.TaskDtos.CreateTaskRequest;
import com.techpulse.taskmanager.task.TaskDtos.TaskResponse;
import com.techpulse.taskmanager.task.TaskDtos.UpdateTaskStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public TaskResponse createTask(@Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(request);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public TaskResponse assignTask(@PathVariable Long id, @Valid @RequestBody AssignTaskRequest request) {
        return taskService.assignTask(id, request.userId());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('USER')")
    public TaskResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateTaskStatusRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser caller) {
        return taskService.updateStatus(id, request.status(), caller);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public List<TaskResponse> listTasks(@AuthenticationPrincipal AuthenticatedUser caller) {
        return taskService.listTasks(caller);
    }
}
