package com.techpulse.taskmanager.task;

import com.techpulse.taskmanager.common.TenantScopedEntity;
import com.techpulse.taskmanager.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_tasks_tenant_assignee", columnList = "tenant_id, assigned_to")
})
public class Task extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Task() {
    }

    public Task(String title, String description, Long tenantId) {
        this.title = title;
        this.description = description;
        this.status = TaskStatus.TODO;
        setTenantId(tenantId);
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Only accepts an assignee from the same tenant as the task. */
    public void assignTo(User user) {
        if (user != null && !user.getTenantId().equals(getTenantId())) {
            throw new IllegalStateException("Cannot assign a task to a user of another tenant");
        }
        this.assignedTo = user;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
