package com.todo.server.commands;

import com.todo.model.Task;
import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import com.todo.server.NotificationService;
import java.util.Map;

public class DeleteTaskCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    private final NotificationService notificationService;
    
    public DeleteTaskCommand(DataStorageInterface storage, 
                           Map<String, String> userSessions,
                           NotificationService notificationService) {
        this.storage = storage;
        this.userSessions = userSessions;
        this.notificationService = notificationService;
    }
    
    @Override
    public Message execute(Object payload, String clientId) {
        String userId = userSessions.get(clientId);
        if (userId == null) {
            return Message.unauthorized("Not logged in");
        }
        
        String taskId = payload.toString().trim();
        Task task = storage.getTaskById(taskId);
        if (task == null) {
            return Message.error("Task not found");
        }
        
        // Store task info before deletion for notification
        String taskTitle = task.getTitle();
        String boardId = task.getBoardId();
        
        storage.deleteTask(taskId);
        
        // Send notification to all board members
        notificationService.notifyTaskDeleted(boardId, taskId, taskTitle);
        
        return Message.success("Task deleted successfully", null);
    }
}
