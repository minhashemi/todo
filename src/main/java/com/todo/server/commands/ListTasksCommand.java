package com.todo.server.commands;

import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import java.util.List;
import java.util.Map;
import com.todo.model.Task;

public class ListTasksCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    private final Map<String, String> clientCurrentBoard;
    
    public ListTasksCommand(DataStorageInterface storage, 
                           Map<String, String> userSessions,
                           Map<String, String> clientCurrentBoard) {
        this.storage = storage;
        this.userSessions = userSessions;
        this.clientCurrentBoard = clientCurrentBoard;
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
        
        List<Task> tasks = storage.getTasksForBoard(currentBoardId);
        return Message.success("Tasks retrieved successfully", tasks);
    }
}
