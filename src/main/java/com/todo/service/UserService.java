package com.todo.service;

import com.todo.model.User;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * UserService handles all user-related operations
 * Manages user registration, authentication, and existence checks
 */
public class UserService {
    // Thread-safe storage for users (username -> User object)
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    /**
     * Registers a new user in the system
     * @param username - unique username
     * @param password - plain text password (will be hashed)
     * @return true if registration successful, false if username already exists
     */
    public boolean register(String username, String password) {
        if (users.containsKey(username)) return false;  // Username already exists
        users.put(username, new User(username, password));  // Create new user
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
        return user != null && user.verifyPassword(password);
    }
    
    /**
     * Checks if a username exists in the system
     * @param username - username to check
     * @return true if user exists, false otherwise
     */
    public boolean userExists(String username) {
        return users.containsKey(username);
    }
}
