package com.todo.service;

import com.todo.model.User;
import com.todo.storage.DatabaseStorage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * UserService handles all user-related operations
 * Manages user registration, authentication, and existence checks
 * Uses database storage for persistence
 */
public class UserService {
    // Thread-safe storage for users (username -> User object)
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final DatabaseStorage database;

    public UserService(DatabaseStorage database) {
        this.database = database;
    }
    
    /**
     * Registers a new user in the system
     * @param username - unique username
     * @param password - plain text password (will be hashed)
     * @return true if registration successful, false if username already exists
     */
    public boolean register(String username, String password) {
        if (database.userExists(username)) return false;  // Username already exists
        User user = new User(username, password);
        database.saveUser(user);  // Save to database
        users.put(username, user);  // Cache in memory
        return true;
    }

    /**
     * Authenticates a user login attempt
     * @param username - username to authenticate
     * @param password - password to verify
     * @return true if credentials are valid, false otherwise
     */
    public boolean login(String username, String password) {
        User user = users.get(username);
        if (user == null) {
            // Load from database if not in cache
            user = database.loadUser(username);
            if (user != null) {
                users.put(username, user);  // Cache for future use
            }
        }
        return user != null && user.verifyPassword(password);
    }

    /**
     * Checks if a username exists in the system
     * @param username - username to check
     * @return true if user exists, false otherwise
     */
    public boolean userExists(String username) {
        if (users.containsKey(username)) return true;
        return database.userExists(username);
    }
}
