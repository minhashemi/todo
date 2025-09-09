package com.todo.server;

import com.todo.core.Command;
import com.todo.server.commands.*;
import com.todo.storage.DataStorageInterface;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandFactory {
    private final Map<String, Command> commands;
    
    public CommandFactory(DataStorageInterface storage, 
                         Map<String, String> userSessions,
                         Map<String, String> clientToUser,
                         Map<String, String> clientCurrentBoard,
                         NotificationService notificationService) {
        this.commands = new ConcurrentHashMap<>();
        initializeCommands(storage, userSessions, clientToUser, clientCurrentBoard, notificationService);
    }
    
    private void initializeCommands(DataStorageInterface storage,
                                   Map<String, String> userSessions,
                                   Map<String, String> clientToUser,
                                   Map<String, String> clientCurrentBoard,
                                   NotificationService notificationService) {
        commands.put("register", new RegisterCommand(storage));
        commands.put("login", new LoginCommand(storage, userSessions, clientToUser));
        commands.put("logout", new LogoutCommand(userSessions, clientToUser));
        commands.put("create_board", new CreateBoardCommand(storage, userSessions, notificationService));
        commands.put("list_boards", new ListBoardsCommand(storage, userSessions));
        commands.put("add_user_to_board", new AddUserToBoardCommand(storage, userSessions, notificationService));
        commands.put("view_board", new ViewBoardCommand(storage, userSessions, clientCurrentBoard));
        commands.put("add_task", new AddTaskCommand(storage, userSessions, clientCurrentBoard, notificationService));
        commands.put("list_tasks", new ListTasksCommand(storage, userSessions, clientCurrentBoard));
        commands.put("update_task_status", new UpdateTaskStatusCommand(storage, userSessions, notificationService));
        commands.put("delete_task", new DeleteTaskCommand(storage, userSessions, notificationService));
    }
    
    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }
}
