package com.todo.service;

import java.io.PrintWriter;
import java.util.Map;

/**
 * NotificationService handles real-time notifications to all connected clients
 * Sends instant updates when boards, users, or tasks are modified
 */
public class NotificationService {
    // Map of client IDs to their output streams for sending notifications
    private final Map<String, PrintWriter> clients;
    
    /**
     * Constructor initializes the notification service
     * @param clients - map of client IDs to their output streams
     */
    public NotificationService(Map<String, PrintWriter> clients) {
        this.clients = clients;
    }
    
    /**
     * Sends a notification to all connected clients
     * @param type - type of notification (board_created, user_added, etc.)
     * @param data - notification data
     */
    public void notifyAll(String type, String data) {
        clients.values().forEach(out -> out.println("NOTIFY:" + type + ":" + data));
    }
    
    /**
     * Notifies all clients when a new board is created
     * @param boardId - ID of the created board
     */
    public void notifyBoardCreated(String boardId) {
        notifyAll("board_created", boardId);
    }
    
    /**
     * Notifies all clients when a user is added to a board
     * @param username - username of the added user
     */
    public void notifyUserAdded(String username) {
        notifyAll("user_added", username);
    }
    
    /**
     * Notifies all clients when a new task is added
     * @param taskId - ID of the added task
     * @param title - title of the added task
     */
    public void notifyTaskAdded(String taskId, String title) {
        notifyAll("task_added", taskId + ":" + title);
    }
    
    /**
     * Notifies all clients when a task status is updated
     * @param taskId - ID of the updated task
     * @param status - new status of the task
     */
    public void notifyTaskUpdated(String taskId, String status) {
        notifyAll("task_updated", taskId + ":" + status);
    }
    
    /**
     * Notifies all clients when a task is deleted
     * @param taskId - ID of the deleted task
     */
    public void notifyTaskDeleted(String taskId) {
        notifyAll("task_deleted", taskId);
    }
}
