package com.todo.protocol;

public class AddTaskPayload {
    private String title;
    private String description;
    private String priority;

    public AddTaskPayload() {}

    public AddTaskPayload(String title, String description, String priority) {
        this.title = title;
        this.description = description;
        this.priority = priority;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(String priority) { this.priority = priority; }
}
