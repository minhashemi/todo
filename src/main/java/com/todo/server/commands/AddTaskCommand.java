package com.todo.server.commands;

import com.todo.model.Board;
import com.todo.model.Task;
import com.todo.protocol.AddTaskPayload;
import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import com.todo.server.NotificationService;
import com.todo.util.GsonUtil;
import com.google.gson.Gson;
import java.util.Map;

public class AddTaskCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    private final Map<String, String> clientCurrentBoard;
    private final NotificationService notificationService;
    
    public AddTaskCommand(DataStorageInterface storage, 
                         Map<String, String> userSessions,
                         Map<String, String> clientCurrentBoard,
                         NotificationService notificationService) {
        this.storage = storage;
        this.userSessions = userSessions;
        this.clientCurrentBoard = clientCurrentBoard;
        this.notificationService = notificationService;
    }
    
    @Override
    public Message execute(Object payload, String clientId) {
        String userId = userSessions.get(clientId);
        if (userId == null) {
            return Message.unauthorized("Not logged in");
        }
        
        String currentBoardId = clientCurrentBoard.get(clientId);
        if (currentBoardId == null) {
            return Message.error("You must be in board view mode. Use 'view_board <boardID>' first.");
        }
        
        Gson gson = GsonUtil.createGson();
        AddTaskPayload taskPayload = gson.fromJson(gson.toJson(payload), AddTaskPayload.class);
        
        String title = taskPayload.getTitle();
        String description = taskPayload.getDescription();
        String priorityStr = taskPayload.getPriority().toUpperCase();
        
        Task.Priority priority;
        try {
            priority = Task.Priority.valueOf(priorityStr);
        } catch (IllegalArgumentException e) {
            return Message.error("Invalid priority. Use: LOW, MEDIUM, HIGH");
        }
        
        Board currentBoard = storage.getBoardById(currentBoardId);
        if (currentBoard == null) {
            return Message.error("Current board not found");
        }
        
        Task task = new Task(title, description, priority, currentBoard.getId());
        storage.addTask(task);
        
        // Send notification to all board members
        notificationService.notifyTaskAdded(currentBoard.getId(), task);
        
        return Message.success("Task created successfully", task.getId());
    }
}
