package com.todo.server.commands;

import com.todo.model.Board;
import com.todo.model.User;
import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import com.todo.server.NotificationService;
import com.todo.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Map;

public class AddUserToBoardCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    private final NotificationService notificationService;
    
    public AddUserToBoardCommand(DataStorageInterface storage, 
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
        
        Gson gson = GsonUtil.createGson();
        JsonObject jsonPayload = gson.fromJson(gson.toJson(payload), JsonObject.class);
        String boardId = jsonPayload.get("boardId").getAsString();
        String targetUserId = jsonPayload.get("userId").getAsString();
        
        Board board = storage.getBoardById(boardId);
        if (board == null) {
            return Message.error("Board not found");
        }
        
        if (!board.isOwner(userId)) {
            return Message.error("You don't own this board");
        }
        
        User targetUser = storage.getUserById(targetUserId);
        if (targetUser == null) {
            return Message.error("Target user not found");
        }
        
        System.out.println("DEBUG: Before adding member - Board members: " + board.getMemberIds());
        board.addMember(targetUserId);
        System.out.println("DEBUG: After adding member - Board members: " + board.getMemberIds());
        
        targetUser.addMemberBoard(boardId);
        storage.addBoard(board);
        storage.addUser(targetUser);
        
        // Verify the board was updated in database
        Board updatedBoard = storage.getBoardById(boardId);
        System.out.println("DEBUG: Board from database after update - Members: " + updatedBoard.getMemberIds());
        
        // Send notification to all board members
        notificationService.notifyUserAddedToBoard(boardId, targetUserId, userId);
        
        return Message.success("User added to board successfully", null);
    }
}
