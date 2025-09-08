package com.todo.storage;

import com.todo.model.Board;
import com.todo.model.Task;
import com.todo.model.User;
import java.util.List;

public interface DataStorageInterface {
    // User operations
    void addUser(User user);
    User getUserById(String id);
    User getUserByUsername(String username);
    
    // Board operations
    void addBoard(Board board);
    Board getBoardById(String id);
    List<Board> getBoardsForUser(String userId);
    
    // Task operations
    void addTask(Task task);
    Task getTaskById(String id);
    List<Task> getTasksForBoard(String boardId);
    void updateTask(Task task);
    void deleteTask(String taskId);
}
