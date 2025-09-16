package com.todo.model;

/**
 * Task model class representing a todo item
 * Manages task properties and status updates
 */
public class Task {
    // Task properties
    private final String id;          // Unique task identifier
    private String title;             // Task title
    private String description;       // Task description
    private String status;            // Current status (TODO, IN_PROGRESS, DONE)
    
    /**
     * Constructor creates a new task with TODO status
     * @param id - unique task ID
     * @param title - task title
     * @param description - task description
     */
    public Task(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = "TODO";  // Default status
    }
    
    /**
     * Updates the task status
     * @param status - new status (TODO, IN_PROGRESS, DONE)
     */
    public void updateStatus(String status) {
        this.status = status;
    }
    
    // Getters for task data
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    
    /**
     * String representation of task for display
     * Format: id:title:status
     * @return formatted task string
     */
    @Override
    public String toString() {
        return id + ":" + title + ":" + status;
    }
}
