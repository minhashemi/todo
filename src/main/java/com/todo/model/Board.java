package com.todo.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Board model class representing a collaborative workspace
 * Manages board members and access permissions
 */
public class Board {
    // Board properties - all final for immutability
    private final String id;              // Unique board identifier
    private final String name;            // Board display name
    private final String owner;           // Username of board owner
    private final Set<String> members;    // Set of member usernames
    
    /**
     * Constructor creates a new board with owner as first member
     * @param id - unique board ID
     * @param name - board display name
     * @param owner - username of board owner
     */
    public Board(String id, String name, String owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.members = new HashSet<>();
        this.members.add(owner);  // Owner is automatically a member
    }
    
    /**
     * Adds a new member to the board
     * @param username - username to add as member
     */
    public void addMember(String username) {
        members.add(username);
    }
    
    /**
     * Checks if user has access to this board
     * @param username - username to check
     * @return true if user is owner or member, false otherwise
     */
    public boolean hasAccess(String username) {
        return owner.equals(username) || members.contains(username);
    }
    
    /**
     * Checks if user is the owner of this board
     * @param username - username to check
     * @return true if user is owner, false otherwise
     */
    public boolean isOwner(String username) {
        return owner.equals(username);
    }
    
    // Getters for board data
    public String getId() { return id; }
    public String getName() { return name; }
    public String getOwner() { return owner; }
    public Set<String> getMembers() { return members; }
}
