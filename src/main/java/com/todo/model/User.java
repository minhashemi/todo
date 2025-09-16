package com.todo.model;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/**
 * User model class representing a user in the system
 * Handles user authentication with secure password hashing
 */
public class User {
    // User credentials - all final for immutability
    private final String username;        // Unique username
    private final String passwordHash;    // SHA-256 hashed password
    private final String salt;            // Random salt for security
    
    /**
     * Constructor creates a new user with hashed password
     * @param username - unique username
     * @param password - plain text password (will be hashed)
     */
    public User(String username, String password) {
        this.username = username;
        this.salt = UUID.randomUUID().toString();  // Generate random salt
        this.passwordHash = hash(password + salt);  // Hash password + salt
    }
    
    /**
     * Verifies if provided password matches stored hash
     * @param password - plain text password to verify
     * @return true if password is correct, false otherwise
     */
    public boolean verifyPassword(String password) {
        return passwordHash.equals(hash(password + salt));
    }
    
    /**
     * Private method to hash input using SHA-256
     * @param input - string to hash
     * @return Base64 encoded hash
     */
    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(input.getBytes()));
        } catch (Exception e) { 
            return input; // Fallback if hashing fails
        }
    }
    
    // Getters for user data
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
}
