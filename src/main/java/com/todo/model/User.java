package com.todo.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class User {
    private String id;
    private String username;
    private String passwordHash;
    private String salt;
    private LocalDateTime createdAt;
    private Set<String> ownedBoards;
    private Set<String> memberBoards;

    public User(String username, String passwordHash, String salt) {
        this.id = generateId();
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.createdAt = LocalDateTime.now();
        this.ownedBoards = new HashSet<>();
        this.memberBoards = new HashSet<>();
    }

    private String generateId() {
        return "user_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Getters and setters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<String> getOwnedBoards() { return ownedBoards; }
    public Set<String> getMemberBoards() { return memberBoards; }

    public void addOwnedBoard(String boardId) {
        ownedBoards.add(boardId);
    }

    public void addMemberBoard(String boardId) {
        memberBoards.add(boardId);
    }

    public boolean hasAccessToBoard(String boardId) {
        return ownedBoards.contains(boardId) || memberBoards.contains(boardId);
    }
}
