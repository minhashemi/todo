package com.todo.service;

import com.todo.model.Board;
import com.todo.model.Task;
import com.todo.storage.DatabaseStorage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * BoardService handles all board and task-related operations
 * Manages board creation, member management, and task CRUD operations
 * Uses database storage for persistence
 */
public class BoardService {
    // Thread-safe storage for boards (boardId -> Board object)
    private final Map<String, Board> boards = new ConcurrentHashMap<>();
    // Thread-safe storage for tasks (boardId -> List of Task objects)
    private final Map<String, List<Task>> tasks = new ConcurrentHashMap<>();
    private final DatabaseStorage database;

    public BoardService(DatabaseStorage database) {
        this.database = database;
    }
    
    /**
     * Creates a new board with the given name and owner
     * @param name - board display name
     * @param owner - username of board owner
     * @return unique board ID
     */
    public String createBoard(String name, String owner) {
        String id = "b" + System.currentTimeMillis();  // Generate unique ID
        Board board = new Board(id, name, owner);
        database.saveBoard(board);  // Save to database
        boards.put(id, board);  // Cache in memory
        tasks.put(id, new ArrayList<>());  // Initialize empty task list
        return id;
    }
    
    /**
     * Gets all boards accessible by a user (owned or member)
     * @param username - username to get boards for
     * @return list of board strings in format "id:name"
     */
    public List<String> getUserBoards(String username) {
        // Load from database if not in cache
        List<Board> userBoards = database.loadUserBoards(username);
        for (Board board : userBoards) {
            boards.put(board.getId(), board);  // Cache for future use
        }
        
        return boards.values().stream()
            .filter(board -> board.hasAccess(username))  // Filter accessible boards
            .map(board -> board.getId() + ":" + board.getName())  // Format as "id:name"
            .collect(Collectors.toList());
    }
    
    /**
     * Adds a user to a board (only board owner can do this)
     * @param boardId - ID of board to add user to
     * @param username - username to add
     * @param requester - username of person making the request
     * @return true if successful, false if not authorized or board not found
     */
    public boolean addUserToBoard(String boardId, String username, String requester) {
        Board board = boards.get(boardId);
        if (board == null) {
            // Load from database if not in cache
            board = database.loadBoard(boardId);
            if (board != null) {
                boards.put(boardId, board);
            }
        }
        if (board == null || !board.isOwner(requester)) return false;  // Check authorization
        board.addMember(username);
        database.saveBoard(board);  // Save updated board to database
        return true;
    }
    
    /**
     * Checks if user can access a specific board
     * @param boardId - ID of board to check
     * @param username - username to check access for
     * @return true if user has access, false otherwise
     */
    public boolean canAccessBoard(String boardId, String username) {
        Board board = boards.get(boardId);
        return board != null && board.hasAccess(username);
    }
    
    /**
     * Gets the name of a board by its ID
     * @param boardId - ID of board
     * @return board name or null if not found
     */
    public String getBoardName(String boardId) {
        Board board = boards.get(boardId);
        return board != null ? board.getName() : null;
    }
    
    /**
     * Gets all tasks for a specific board
     * @param boardId - ID of board
     * @return list of tasks (empty list if board not found)
     */
    public List<Task> getTasks(String boardId) {
        List<Task> boardTasks = tasks.get(boardId);
        if (boardTasks == null) {
            // Load from database if not in cache
            boardTasks = database.loadBoardTasks(boardId);
            tasks.put(boardId, boardTasks);
        }
        return boardTasks;
    }

    /**
     * Adds a new task to a board
     * @param boardId - ID of board to add task to
     * @param task - task object to add
     */
    public void addTask(String boardId, Task task) {
        database.saveTask(task, boardId);  // Save to database
        tasks.computeIfAbsent(boardId, k -> new ArrayList<>()).add(task);
    }

    /**
     * Updates the status of a specific task
     * @param boardId - ID of board containing the task
     * @param taskId - ID of task to update
     * @param status - new status (TODO, IN_PROGRESS, DONE)
     * @return true if task found and updated, false otherwise
     */
    public boolean updateTaskStatus(String boardId, String taskId, String status) {
        if (database.updateTaskStatus(taskId, status)) {
            // Update in memory cache
            List<Task> boardTasks = tasks.get(boardId);
            if (boardTasks != null) {
                boardTasks.stream()
                    .filter(task -> task.getId().equals(taskId))
                    .findFirst()
                    .ifPresent(task -> task.updateStatus(status));
            }
            return true;
        }
        return false;
    }

    /**
     * Deletes a task from a board
     * @param boardId - ID of board containing the task
     * @param taskId - ID of task to delete
     * @return true if task found and deleted, false otherwise
     */
    public boolean deleteTask(String boardId, String taskId) {
        if (database.deleteTask(taskId)) {
            // Remove from memory cache
            List<Task> boardTasks = tasks.get(boardId);
            if (boardTasks != null) {
                boardTasks.removeIf(task -> task.getId().equals(taskId));
            }
            return true;
        }
        return false;
    }
}
