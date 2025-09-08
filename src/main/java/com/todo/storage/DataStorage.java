package com.todo.storage;

import com.todo.model.Board;
import com.todo.model.Task;
import com.todo.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataStorage implements DataStorageInterface {
    private static final String DATABASE_URL = "jdbc:sqlite:todo.db";
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Connection connection;

    public DataStorage() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DATABASE_URL);
            createTables();
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        // Create users table
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
            "id TEXT PRIMARY KEY, " +
            "username TEXT UNIQUE NOT NULL, " +
            "password_hash TEXT NOT NULL, " +
            "salt TEXT NOT NULL, " +
            "created_at TEXT NOT NULL" +
            ")";
        
        // Create boards table
        String createBoardsTable = "CREATE TABLE IF NOT EXISTS boards (" +
            "id TEXT PRIMARY KEY, " +
            "name TEXT NOT NULL, " +
            "owner_id TEXT NOT NULL, " +
            "created_at TEXT NOT NULL, " +
            "FOREIGN KEY (owner_id) REFERENCES users(id)" +
            ")";
        
        // Create board_members table (many-to-many relationship)
        String createBoardMembersTable = "CREATE TABLE IF NOT EXISTS board_members (" +
            "board_id TEXT NOT NULL, " +
            "user_id TEXT NOT NULL, " +
            "PRIMARY KEY (board_id, user_id), " +
            "FOREIGN KEY (board_id) REFERENCES boards(id), " +
            "FOREIGN KEY (user_id) REFERENCES users(id)" +
            ")";
        
        // Create tasks table
        String createTasksTable = "CREATE TABLE IF NOT EXISTS tasks (" +
            "id TEXT PRIMARY KEY, " +
            "title TEXT NOT NULL, " +
            "description TEXT, " +
            "status TEXT NOT NULL, " +
            "priority TEXT NOT NULL, " +
            "board_id TEXT NOT NULL, " +
            "created_at TEXT NOT NULL, " +
            "FOREIGN KEY (board_id) REFERENCES boards(id)" +
            ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createBoardsTable);
            stmt.execute(createBoardMembersTable);
            stmt.execute(createTasksTable);
        }
    }

    // User operations
    public void addUser(User user) {
        lock.writeLock().lock();
        try {
            String sql = "INSERT INTO users (id, username, password_hash, salt, created_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, user.getId());
                stmt.setString(2, user.getUsername());
                stmt.setString(3, user.getPasswordHash());
                stmt.setString(4, user.getSalt());
                stmt.setString(5, user.getCreatedAt().toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public User getUserById(String id) {
        lock.readLock().lock();
        try {
            String sql = "SELECT * FROM users WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return createUserFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by id: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public User getUserByUsername(String username) {
        lock.readLock().lock();
        try {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return createUserFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by username: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    private User createUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User(
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("salt")
        );
        user.setId(rs.getString("id"));
        user.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        return user;
    }

    // Board operations
    public void addBoard(Board board) {
        lock.writeLock().lock();
        try {
            // Insert board
            String sql = "INSERT INTO boards (id, name, owner_id, created_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, board.getId());
                stmt.setString(2, board.getName());
                stmt.setString(3, board.getOwnerId());
                stmt.setString(4, board.getCreatedAt().toString());
                stmt.executeUpdate();
            }
            
            // Add owner as member
            addBoardMember(board.getId(), board.getOwnerId());
        } catch (SQLException e) {
            System.err.println("Error adding board: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Board getBoardById(String id) {
        lock.readLock().lock();
        try {
            String sql = "SELECT * FROM boards WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Board board = createBoardFromResultSet(rs);
                        loadBoardMembers(board);
                        return board;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting board by id: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public List<Board> getBoardsForUser(String userId) {
        lock.readLock().lock();
        try {
            String sql = "SELECT DISTINCT b.* FROM boards b " +
                "LEFT JOIN board_members bm ON b.id = bm.board_id " +
                "WHERE b.owner_id = ? OR bm.user_id = ?";
            List<Board> userBoards = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, userId);
                stmt.setString(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Board board = createBoardFromResultSet(rs);
                        loadBoardMembers(board);
                        userBoards.add(board);
                    }
                }
            }
            return userBoards;
        } catch (SQLException e) {
            System.err.println("Error getting boards for user: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return new ArrayList<>();
    }

    private Board createBoardFromResultSet(ResultSet rs) throws SQLException {
        Board board = new Board(rs.getString("name"), rs.getString("owner_id"));
        board.setId(rs.getString("id"));
        board.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        return board;
    }

    private void loadBoardMembers(Board board) {
        try {
            String sql = "SELECT user_id FROM board_members WHERE board_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, board.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        board.addMember(rs.getString("user_id"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading board members: " + e.getMessage());
        }
    }

    private void addBoardMember(String boardId, String userId) {
        try {
            String sql = "INSERT OR IGNORE INTO board_members (board_id, user_id) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, boardId);
                stmt.setString(2, userId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error adding board member: " + e.getMessage());
        }
    }

    // Task operations
    public void addTask(Task task) {
        lock.writeLock().lock();
        try {
            String sql = "INSERT INTO tasks (id, title, description, status, priority, board_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, task.getId());
                stmt.setString(2, task.getTitle());
                stmt.setString(3, task.getDescription());
                stmt.setString(4, task.getStatus().name());
                stmt.setString(5, task.getPriority().name());
                stmt.setString(6, task.getBoardId());
                stmt.setString(7, task.getCreatedAt().toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error adding task: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Task getTaskById(String id) {
        lock.readLock().lock();
        try {
            String sql = "SELECT * FROM tasks WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return createTaskFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting task by id: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public List<Task> getTasksForBoard(String boardId) {
        lock.readLock().lock();
        try {
            String sql = "SELECT * FROM tasks WHERE board_id = ? ORDER BY created_at ASC";
            List<Task> boardTasks = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, boardId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        boardTasks.add(createTaskFromResultSet(rs));
                    }
                }
            }
            return boardTasks;
        } catch (SQLException e) {
            System.err.println("Error getting tasks for board: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return new ArrayList<>();
    }

    public void updateTask(Task task) {
        lock.writeLock().lock();
        try {
            String sql = "UPDATE tasks SET title = ?, description = ?, status = ?, priority = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, task.getTitle());
                stmt.setString(2, task.getDescription());
                stmt.setString(3, task.getStatus().name());
                stmt.setString(4, task.getPriority().name());
                stmt.setString(5, task.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error updating task: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteTask(String taskId) {
        lock.writeLock().lock();
        try {
            String sql = "DELETE FROM tasks WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, taskId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error deleting task: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Task createTaskFromResultSet(ResultSet rs) throws SQLException {
        Task task = new Task(
            rs.getString("title"),
            rs.getString("description"),
            Task.Priority.valueOf(rs.getString("priority")),
            rs.getString("board_id")
        );
        task.setId(rs.getString("id"));
        task.setStatus(Task.Status.valueOf(rs.getString("status")));
        task.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        return task;
    }
}
