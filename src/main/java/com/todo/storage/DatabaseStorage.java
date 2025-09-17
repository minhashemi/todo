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

/**
 * Database storage implementation using SQLite
 * Provides persistent storage for users, boards, and tasks
 * Thread-safe with proper concurrency control
 */
public class DatabaseStorage {
    private static final String DATABASE_URL = "jdbc:sqlite:todo.db";
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Connection connection;

    public DatabaseStorage() {
        try {
            connection = DriverManager.getConnection(DATABASE_URL);
            createTables();
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Creates database tables if they don't exist
     */
    private void createTables() throws SQLException {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
            "id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, " +
            "password_hash TEXT NOT NULL, salt TEXT NOT NULL, created_at TEXT NOT NULL)";
        
        String createBoardsTable = "CREATE TABLE IF NOT EXISTS boards (" +
            "id TEXT PRIMARY KEY, name TEXT NOT NULL, owner_id TEXT NOT NULL, " +
            "created_at TEXT NOT NULL, FOREIGN KEY (owner_id) REFERENCES users(id))";
        
        String createBoardMembersTable = "CREATE TABLE IF NOT EXISTS board_members (" +
            "board_id TEXT NOT NULL, user_id TEXT NOT NULL, " +
            "PRIMARY KEY (board_id, user_id), " +
            "FOREIGN KEY (board_id) REFERENCES boards(id), " +
            "FOREIGN KEY (user_id) REFERENCES users(id))";
        
        String createTasksTable = "CREATE TABLE IF NOT EXISTS tasks (" +
            "id TEXT PRIMARY KEY, title TEXT NOT NULL, description TEXT, " +
            "status TEXT NOT NULL, priority TEXT NOT NULL, board_id TEXT NOT NULL, " +
            "created_at TEXT NOT NULL, FOREIGN KEY (board_id) REFERENCES boards(id))";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createBoardsTable);
            stmt.execute(createBoardMembersTable);
            stmt.execute(createTasksTable);
        }
    }

    // ==================== USER OPERATIONS ====================

    /**
     * Saves a user to the database
     */
    public void saveUser(User user) {
        lock.writeLock().lock();
        try {
            String sql = "INSERT OR REPLACE INTO users (id, username, password_hash, salt, created_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getUsername());
                stmt.setString(3, user.getPasswordHash());
                stmt.setString(4, user.getSalt());
                stmt.setString(5, LocalDateTime.now().toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error saving user: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads a user from the database
     */
    public User loadUser(String username) {
        lock.readLock().lock();
        try {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new User(rs.getString("username"), rs.getString("password_hash"), rs.getString("salt"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading user: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    /**
     * Checks if a user exists in the database
     */
    public boolean userExists(String username) {
        lock.readLock().lock();
        try {
            String sql = "SELECT 1 FROM users WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username);
                return stmt.executeQuery().next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking user existence: " + e.getMessage());
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== BOARD OPERATIONS ====================

    /**
     * Saves a board to the database
     */
    public void saveBoard(Board board) {
        lock.writeLock().lock();
        try {
            // Save board
            String sql = "INSERT OR REPLACE INTO boards (id, name, owner_id, created_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, board.getId());
                stmt.setString(2, board.getName());
                stmt.setString(3, board.getOwner());
                stmt.setString(4, LocalDateTime.now().toString());
                stmt.executeUpdate();
            }

            // Save board members
            String memberSql = "INSERT OR REPLACE INTO board_members (board_id, user_id) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(memberSql)) {
                for (String member : board.getMembers()) {
                    stmt.setString(1, board.getId());
                    stmt.setString(2, member);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving board: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads a board from the database
     */
    public Board loadBoard(String boardId) {
        lock.readLock().lock();
        try {
            String sql = "SELECT * FROM boards WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, boardId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Board board = new Board(rs.getString("id"), rs.getString("name"), rs.getString("owner_id"));
                    
                    // Load members
                    String memberSql = "SELECT user_id FROM board_members WHERE board_id = ?";
                    try (PreparedStatement memberStmt = connection.prepareStatement(memberSql)) {
                        memberStmt.setString(1, boardId);
                        ResultSet memberRs = memberStmt.executeQuery();
                        while (memberRs.next()) {
                            board.addMember(memberRs.getString("user_id"));
                        }
                    }
                    return board;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading board: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    /**
     * Loads all boards for a user
     */
    public List<Board> loadUserBoards(String username) {
        List<Board> boards = new ArrayList<>();
        lock.readLock().lock();
        try {
            String sql = "SELECT b.* FROM boards b " +
                        "LEFT JOIN board_members bm ON b.id = bm.board_id " +
                        "WHERE b.owner_id = ? OR bm.user_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, username);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Board board = new Board(rs.getString("id"), rs.getString("name"), rs.getString("owner_id"));
                    
                    // Load members for this board
                    String memberSql = "SELECT user_id FROM board_members WHERE board_id = ?";
                    try (PreparedStatement memberStmt = connection.prepareStatement(memberSql)) {
                        memberStmt.setString(1, board.getId());
                        ResultSet memberRs = memberStmt.executeQuery();
                        while (memberRs.next()) {
                            board.addMember(memberRs.getString("user_id"));
                        }
                    }
                    boards.add(board);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading user boards: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return boards;
    }

    // ==================== TASK OPERATIONS ====================

    /**
     * Saves a task to the database
     */
    public void saveTask(Task task, String boardId) {
        lock.writeLock().lock();
        try {
            String sql = "INSERT OR REPLACE INTO tasks (id, title, description, status, priority, board_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, task.getId());
                stmt.setString(2, task.getTitle());
                stmt.setString(3, task.getDescription());
                stmt.setString(4, task.getStatus());
                stmt.setString(5, "MEDIUM"); // Default priority
                stmt.setString(6, boardId);
                stmt.setString(7, LocalDateTime.now().toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error saving task: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads all tasks for a board
     */
    public List<Task> loadBoardTasks(String boardId) {
        List<Task> tasks = new ArrayList<>();
        lock.readLock().lock();
        try {
            String sql = "SELECT * FROM tasks WHERE board_id = ? ORDER BY created_at";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, boardId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Task task = new Task(rs.getString("id"), rs.getString("title"), rs.getString("description"));
                    task.updateStatus(rs.getString("status"));
                    tasks.add(task);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading board tasks: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
        return tasks;
    }

    /**
     * Updates a task status in the database
     */
    public boolean updateTaskStatus(String taskId, String status) {
        lock.writeLock().lock();
        try {
            String sql = "UPDATE tasks SET status = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setString(2, taskId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error updating task status: " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Deletes a task from the database
     */
    public boolean deleteTask(String taskId) {
        lock.writeLock().lock();
        try {
            String sql = "DELETE FROM tasks WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, taskId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting task: " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Closes the database connection
     */
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }
}
