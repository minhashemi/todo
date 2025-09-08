package com.todo.server.commands;

import com.todo.protocol.Message;
import com.todo.core.Command;
import java.util.Map;

public class LogoutCommand implements Command {
    private final Map<String, String> userSessions;
    private final Map<String, String> clientToUser;
    
    public LogoutCommand(Map<String, String> userSessions, Map<String, String> clientToUser) {
        this.userSessions = userSessions;
        this.clientToUser = clientToUser;
    }
    
    @Override
    public Message execute(Object payload, String clientId) {
        userSessions.remove(clientId);
        clientToUser.remove(clientId);
        return Message.success("Logged out successfully", null);
    }
}
