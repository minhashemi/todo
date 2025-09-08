package com.todo.server.commands;

import com.todo.model.Board;
import com.todo.model.User;
import com.todo.protocol.CreateBoardPayload;
import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import com.todo.util.GsonUtil;
import com.google.gson.Gson;
import java.util.Map;

public class CreateBoardCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    
    public CreateBoardCommand(DataStorageInterface storage, Map<String, String> userSessions) {
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
        CreateBoardPayload boardPayload = gson.fromJson(gson.toJson(payload), CreateBoardPayload.class);
        String boardName = boardPayload.getBoardName();
        
        Board board = new Board(boardName, userId);
        storage.addBoard(board);
        
        User user = storage.getUserById(userId);
        user.addOwnedBoard(board.getId());
        storage.addUser(user);
        
        return Message.success("Board created successfully", board.getId());
    }
}
