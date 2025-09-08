package com.todo.server.commands;

import com.todo.model.Task;
import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import com.todo.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Map;

public class UpdateTaskStatusCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    
    public UpdateTaskStatusCommand(DataStorageInterface storage, Map<String, String> userSessions) {
        this.storage = storage;
        this.userSessions = userSessions;
    }
    
    @Override
    public Message execute(Object payload, String clientId) {
        String userId = userSessions.get(clientId);
        if (userId == null) {
            return Message.unauthorized("Not logged in");
        }
        
        Gson gson = GsonUtil.createGson();
        JsonObject jsonPayload = gson.fromJson(gson.toJson(payload), JsonObject.class);
        String taskId = jsonPayload.get("taskId").getAsString();
        String statusStr = jsonPayload.get("status").getAsString().toUpperCase();
        
        Task.Status status;
        try {
            status = Task.Status.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return Message.error("Invalid status. Use: TODO, IN_PROGRESS, DONE");
        }
        
        Task task = storage.getTaskById(taskId);
        if (task == null) {
            return Message.error("Task not found");
        }
        
        task.setStatus(status);
        storage.updateTask(task);
        
        return Message.success("Task status updated successfully", null);
    }
}
