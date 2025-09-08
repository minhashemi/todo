package com.todo.server.commands;

import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import java.util.List;
import java.util.Map;
import com.todo.model.Board;

public class ListBoardsCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    
    public ListBoardsCommand(DataStorageInterface storage, Map<String, String> userSessions) {
        this.storage = storage;
        this.userSessions = userSessions;
    }
    
    @Override
    public Message execute(Object payload, String clientId) {
        String userId = userSessions.get(clientId);
        if (userId == null) {
            return Message.unauthorized("Not logged in");
        }
        
        List<Board> boards = storage.getBoardsForUser(userId);
        return Message.success("Boards retrieved successfully", boards);
    }
}
