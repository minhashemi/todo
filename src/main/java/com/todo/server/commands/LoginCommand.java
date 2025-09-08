package com.todo.server.commands;

import com.todo.model.User;
import com.todo.protocol.LoginPayload;
import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import com.todo.util.GsonUtil;
import com.todo.util.SecurityUtil;
import com.google.gson.Gson;
import java.util.Map;

public class LoginCommand implements Command {
    private final DataStorageInterface storage;
    private final Map<String, String> userSessions;
    private final Map<String, String> clientToUser;
    
    public LoginCommand(DataStorageInterface storage, 
                       Map<String, String> userSessions,
                       Map<String, String> clientToUser) {
        this.storage = storage;
        this.userSessions = userSessions;
        this.clientToUser = clientToUser;
    }
    
    @Override
    public Message execute(Object payload, String clientId) {
        Gson gson = GsonUtil.createGson();
        LoginPayload loginPayload = gson.fromJson(gson.toJson(payload), LoginPayload.class);
        
        String username = loginPayload.getUsername();
        String password = loginPayload.getPassword();
        
        User user = storage.getUserByUsername(username);
        if (user == null) {
            return Message.unauthorized("User not found");
        }
        
        if (!SecurityUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash())) {
            return Message.unauthorized("Invalid password");
        }
        
        userSessions.put(clientId, user.getId());
        clientToUser.put(clientId, user.getId());
        return Message.success("Login successful", user.getId());
    }
}
