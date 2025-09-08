package com.todo.server.commands;

import com.todo.model.User;
import com.todo.protocol.LoginPayload;
import com.todo.protocol.Message;
import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import com.todo.util.GsonUtil;
import com.todo.util.SecurityUtil;
import com.google.gson.Gson;

public class RegisterCommand implements Command {
    private final DataStorageInterface storage;
    
    public RegisterCommand(DataStorageInterface storage) {
        this.storage = storage;
    }
    
    @Override
    public Message execute(Object payload, String clientId) {
        try {
            Gson gson = GsonUtil.createGson();
            LoginPayload loginPayload = gson.fromJson(gson.toJson(payload), LoginPayload.class);
            
            String username = loginPayload.getUsername();
            String password = loginPayload.getPassword();
            
            if (storage.getUserByUsername(username) != null) {
                return Message.error("Username already exists");
            }
            
            String salt = SecurityUtil.generateSalt();
            String passwordHash = SecurityUtil.hashPassword(password, salt);
            User user = new User(username, passwordHash, salt);
            storage.addUser(user);
            
            return Message.success("User registered successfully", null);
        } catch (Exception e) {
            return Message.error("Error in register: " + e.getMessage());
        }
    }
}
