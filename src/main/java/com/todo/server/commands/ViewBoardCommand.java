package com.todo.server.commands;

import com.todo.model.Board;
import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import java.util.Map;

public class ViewBoardCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    private final Map<String, String> clientCurrentBoard;
    
    public ViewBoardCommand(DataStorageInterface storage, 
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
        
        String boardId = payload.toString().trim();
        Board board = storage.getBoardById(boardId);
        if (board == null) {
            return Message.error("Board not found");
        }
        
        if (!board.isMember(userId)) {
            return Message.error("Access denied");
        }
        
        clientCurrentBoard.put(clientId, boardId);
        return Message.success("Entering board view mode for: " + board.getName(), board);
    }
}
