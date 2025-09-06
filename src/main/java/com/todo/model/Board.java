package com.todo.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Board {
    private String id;
    private String name;
    private String ownerId;
    private LocalDateTime createdAt;
    private Set<String> memberIds;

    public Board(String name, String ownerId) {
        this.id = generateId();
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = LocalDateTime.now();
        this.memberIds = new HashSet<>();
        this.memberIds.add(ownerId); // Owner is automatically a member
    }

    private String generateId() {
        return "board_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getOwnerId() { return ownerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<String> getMemberIds() { return memberIds; }

    public void addMember(String userId) {
        memberIds.add(userId);
    }

    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }

    public boolean isOwner(String userId) {
        return ownerId.equals(userId);
    }
}
