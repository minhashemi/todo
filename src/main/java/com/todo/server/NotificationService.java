package com.todo.server;

import com.todo.protocol.Message;
import com.todo.storage.DataStorageInterface;
import com.todo.model.Board;
import com.todo.model.Task;
import com.todo.model.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationService {
    private final Map<String, PrintWriter> connectedClients;
    private final Map<String, String> clientToUser;
    private final Map<String, String> userSessions;
    private final DataStorageInterface storage;
    private final int udpPort;
    
    public NotificationService(Map<String, PrintWriter> connectedClients,
                             Map<String, String> clientToUser,
                             Map<String, String> userSessions,
                             DataStorageInterface storage,
                             int udpPort) {
        this.connectedClients = connectedClients;
        this.clientToUser = clientToUser;
        this.userSessions = userSessions;
        this.storage = storage;
        this.udpPort = udpPort;
    }
    
    /**
     * Broadcast a notification to all users who have access to a specific board
     */
    public void broadcastBoardNotification(String boardId, String notificationType, Object data) {
        Board board = storage.getBoardById(boardId);
        if (board == null) return;
        
        // Get all users who have access to this board
        Set<String> boardMembers = board.getMemberIds();
        
        // Find all connected clients that belong to board members
        for (Map.Entry<String, String> entry : clientToUser.entrySet()) {
            String clientId = entry.getKey();
            String userId = entry.getValue();
            
            if (boardMembers.contains(userId)) {
                sendNotificationToClient(clientId, notificationType, data);
            }
        }
    }
    
    /**
     * Send a notification to a specific user (all their connected clients)
     */
    public void sendNotificationToUser(String userId, String notificationType, Object data) {
        // Find all client IDs for this user (multiple clients can be logged in as same user)
        int clientCount = 0;
        for (Map.Entry<String, String> entry : clientToUser.entrySet()) {
            if (userId.equals(entry.getValue())) {
                sendNotificationToClient(entry.getKey(), notificationType, data);
                clientCount++;
            }
        }
        System.out.println("DEBUG: Sent " + notificationType + " notification to " + clientCount + " clients for user " + userId);
    }
    
    /**
     * Send a notification to a specific client
     */
    private void sendNotificationToClient(String clientId, String notificationType, Object data) {
        PrintWriter clientOut = connectedClients.get(clientId);
        if (clientOut != null) {
            Message notification = new Message();
            notification.setStatus("notification");
            notification.setMessage(notificationType);
            
            // Convert data to JSON string to avoid serialization issues
            if (data != null) {
                try {
                    com.google.gson.Gson gson = com.todo.util.GsonUtil.createGson();
                    String dataJson = gson.toJson(data);
                    notification.setData(dataJson);
                } catch (Exception e) {
                    System.err.println("Error serializing notification data: " + e.getMessage());
                    notification.setData(data.toString());
                }
            } else {
                notification.setData(null);
            }
            
            try {
                clientOut.println(notification.toJson());
            } catch (Exception e) {
                System.err.println("Error sending notification to client " + clientId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Send UDP notification for lightweight updates
     */
    public void sendUdpNotification(String message) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                buffer, 
                buffer.length, 
                InetAddress.getByName("localhost"), 
                udpPort
            );
            udpSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending UDP notification: " + e.getMessage());
        }
    }
    
    /**
     * Notify board members when a task is added
     */
    public void notifyTaskAdded(String boardId, Task task) {
        String message = "New task added: " + task.getTitle() + " (" + task.getPriority() + ")";
        broadcastBoardNotification(boardId, "task_added", task);
        sendUdpNotification("TASK_ADDED:" + boardId + ":" + task.getTitle());
    }
    
    /**
     * Notify board members when a task is updated
     */
    public void notifyTaskUpdated(String boardId, Task task) {
        String message = "Task updated: " + task.getTitle() + " -> " + task.getStatus();
        broadcastBoardNotification(boardId, "task_updated", task);
        sendUdpNotification("TASK_UPDATED:" + boardId + ":" + task.getTitle() + ":" + task.getStatus());
    }
    
    /**
     * Notify board members when a task is deleted
     */
    public void notifyTaskDeleted(String boardId, String taskId, String taskTitle) {
        String message = "Task deleted: " + taskTitle;
        broadcastBoardNotification(boardId, "task_deleted", taskId);
        sendUdpNotification("TASK_DELETED:" + boardId + ":" + taskTitle);
    }
    
    /**
     * Notify board members when a user is added to the board
     */
    public void notifyUserAddedToBoard(String boardId, String newUserId) {
        User newUser = storage.getUserById(newUserId);
        if (newUser != null) {
            String message = "User " + newUser.getUsername() + " added to board";
            broadcastBoardNotification(boardId, "user_added", newUser.getUsername());
            sendUdpNotification("USER_ADDED:" + boardId + ":" + newUser.getUsername());
        }
    }
    
    /**
     * Notify when a board is created
     */
    public void notifyBoardCreated(String boardId, String creatorId) {
        Board board = storage.getBoardById(boardId);
        if (board != null) {
            sendNotificationToUser(creatorId, "board_created", board);
        }
    }
}
