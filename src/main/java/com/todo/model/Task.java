package com.todo.model;

import java.time.LocalDateTime;

public class Task {
    public enum Status {
        TODO, IN_PROGRESS, DONE
    }

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    private String id;
    private String title;
    private String description;
    private Status status;
    private Priority priority;
    private String boardId;
    private LocalDateTime createdAt;

    public Task(String title, String description, Priority priority, String boardId) {
        this.id = generateId();
        this.title = title;
        this.description = description;
        this.status = Status.TODO;
        this.priority = priority;
        this.boardId = boardId;
        this.createdAt = LocalDateTime.now();
    }

    private String generateId() {
        return "task_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Status getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public String getBoardId() { return boardId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}
