package com.todo.server.commands;

import com.todo.model.Task;
import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import java.util.Map;

public class DeleteTaskCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    
    public DeleteTaskCommand(DataStorageInterface storage, Map<String, String> userSessions) {
        this.storage = storage;
        this.userSessions = userSessions;
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
        
        storage.deleteTask(taskId);
        return Message.success("Task deleted successfully", null);
    }
}
