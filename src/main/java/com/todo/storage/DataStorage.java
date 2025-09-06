package com.todo.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.todo.model.Board;
import com.todo.model.Task;
import com.todo.model.User;
import com.todo.util.GsonUtil;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataStorage {
    private static final String USERS_FILE = "users.json";
    private static final String BOARDS_FILE = "boards.json";
    private static final String TASKS_FILE = "tasks.json";
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Gson gson = GsonUtil.createGson();
    
    private Map<String, User> users = new HashMap<>();
    private Map<String, Board> boards = new HashMap<>();
    private Map<String, Task> tasks = new HashMap<>();

    public DataStorage() {
        loadData();
    }

    public void saveData() {
        lock.writeLock().lock();
        try {
            saveToFile(users, USERS_FILE);
            saveToFile(boards, BOARDS_FILE);
            saveToFile(tasks, TASKS_FILE);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadData() {
        lock.writeLock().lock();
        try {
            users = loadFromFile(USERS_FILE, new TypeToken<Map<String, User>>(){}.getType());
            boards = loadFromFile(BOARDS_FILE, new TypeToken<Map<String, Board>>(){}.getType());
            tasks = loadFromFile(TASKS_FILE, new TypeToken<Map<String, Task>>(){}.getType());
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T> void saveToFile(T data, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("Error saving data to " + filename + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T loadFromFile(String filename, Type type) {
        File file = new File(filename);
        if (!file.exists()) {
            return (T) new HashMap<String, Object>();
        }
        
        try (FileReader reader = new FileReader(filename)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            System.err.println("Error loading data from " + filename + ": " + e.getMessage());
            return (T) new HashMap<String, Object>();
        }
    }

    // User operations
    public void addUser(User user) {
        lock.writeLock().lock();
        try {
            users.put(user.getId(), user);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public User getUserById(String id) {
        lock.readLock().lock();
        try {
            return users.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public User getUserByUsername(String username) {
        lock.readLock().lock();
        try {
            return users.values().stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    // Board operations
    public void addBoard(Board board) {
        lock.writeLock().lock();
        try {
            boards.put(board.getId(), board);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Board getBoardById(String id) {
        lock.readLock().lock();
        try {
            return boards.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Board> getBoardsForUser(String userId) {
        lock.readLock().lock();
        try {
            List<Board> userBoards = new ArrayList<>();
            for (Board board : boards.values()) {
                if (board.isMember(userId)) {
                    userBoards.add(board);
                }
            }
            return userBoards;
        } finally {
            lock.readLock().unlock();
        }
    }

    // Task operations
    public void addTask(Task task) {
        lock.writeLock().lock();
        try {
            tasks.put(task.getId(), task);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Task getTaskById(String id) {
        lock.readLock().lock();
        try {
            return tasks.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Task> getTasksForBoard(String boardId) {
        lock.readLock().lock();
        try {
            List<Task> boardTasks = new ArrayList<>();
            for (Task task : tasks.values()) {
                if (task.getBoardId().equals(boardId)) {
                    boardTasks.add(task);
                }
            }
            boardTasks.sort((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()));
            return boardTasks;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void updateTask(Task task) {
        lock.writeLock().lock();
        try {
            tasks.put(task.getId(), task);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteTask(String taskId) {
        lock.writeLock().lock();
        try {
            tasks.remove(taskId);
            saveData();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
