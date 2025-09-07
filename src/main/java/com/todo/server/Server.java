package com.todo.server;

import com.todo.storage.DataStorage;
import com.todo.model.User;
import com.todo.model.Board;
import com.todo.model.Task;
import com.todo.protocol.Message;
import com.todo.protocol.LoginPayload;
import com.todo.protocol.CreateBoardPayload;
import com.todo.protocol.AddTaskPayload;
import com.todo.util.SecurityUtil;
import com.todo.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int TCP_PORT = 8080;
    private static final int UDP_PORT = 8081;
    
    private final DataStorage storage;
    private final Map<String, PrintWriter> connectedClients;
    private final Map<String, String> userSessions;
    private final Map<String, String> clientToUser;
    private final Map<String, String> clientCurrentBoard;
    private final ExecutorService threadPool;
    private boolean running;

    public Server() {
        this.storage = new DataStorage();
        this.connectedClients = new ConcurrentHashMap<>();
        this.userSessions = new ConcurrentHashMap<>();
        this.clientToUser = new ConcurrentHashMap<>();
        this.clientCurrentBoard = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.running = false;
    }

    public void start() {
        running = true;
        System.out.println("Starting Todo Server...");
        
        // Start TCP server for main operations
        threadPool.submit(this::startTcpServer);
        
        // Start UDP server for notifications
        threadPool.submit(this::startUdpServer);
        
        System.out.println("Server started on TCP port " + TCP_PORT + " and UDP port " + UDP_PORT);
    }

    private void startTcpServer() {
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("TCP Server error: " + e.getMessage());
            }
        }
    }

    private void startUdpServer() {
        try (DatagramSocket udpSocket = new DatagramSocket(UDP_PORT)) {
            byte[] buffer = new byte[1024];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                threadPool.submit(() -> handleUdpMessage(packet));
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("UDP Server error: " + e.getMessage());
            }
        }
    }

    private void handleUdpMessage(DatagramPacket packet) {
        String message = new String(packet.getData(), 0, packet.getLength());
        System.out.println("UDP message received: " + message);
        // UDP is used for lightweight notifications
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientId;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientId = "client_" + System.currentTimeMillis();
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                
                connectedClients.put(clientId, out);
                
                String inputLine;
                while ((inputLine = in.readLine()) != null && running) {
                    Message response = processMessage(inputLine);
                    out.println(response.toJson());
                }
            } catch (IOException e) {
                System.err.println("Client handler error: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            connectedClients.remove(clientId);
            if (clientId != null) {
                userSessions.remove(clientId);
                clientToUser.remove(clientId);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }

        private Message processMessage(String jsonMessage) {
            try {
                Message message = Message.fromJson(jsonMessage);
                String command = message.getCommand();
                Object payload = message.getPayload();

                switch (command) {
                    case "register":
                        return handleRegister(payload);
                    case "login":
                        return handleLogin(payload);
                    case "logout":
                        return handleLogout();
                    case "create_board":
                        return handleCreateBoard(payload);
                    case "list_boards":
                        return handleListBoards();
                    case "add_user_to_board":
                        return handleAddUserToBoard(payload);
                    case "view_board":
                        return handleViewBoard(payload);
                    case "add_task":
                        return handleAddTask(payload);
                    case "list_tasks":
                        return handleListTasks();
                    case "update_task_status":
                        return handleUpdateTaskStatus(payload);
                    case "delete_task":
                        return handleDeleteTask(payload);
                    default:
                        return Message.error("Unknown command: " + command);
                }
            } catch (Exception e) {
                return Message.error("Server error: " + e.getMessage());
            }
        }

        private Message handleRegister(Object payload) {
            try {
                Gson gson = GsonUtil.createGson();
                LoginPayload loginPayload = gson.fromJson(gson.toJson(payload), LoginPayload.class);
                
                String username = loginPayload.getUsername();
                String password = loginPayload.getPassword();
                
                if (storage.getUserByUsername(username) != null) {
                    return Message.error("Username already exists");
                }
                
                String salt = SecurityUtil.generateSalt();
                String passwordHash = SecurityUtil.hashPassword(password, salt);
                User user = new User(username, passwordHash, salt);
                storage.addUser(user);
                
                return Message.success("User registered successfully", null);
            } catch (Exception e) {
                return Message.error("Error in register: " + e.getMessage());
            }
        }

        private Message handleLogin(Object payload) {
            Gson gson = GsonUtil.createGson();
            LoginPayload loginPayload = gson.fromJson(gson.toJson(payload), LoginPayload.class);
            
            String username = loginPayload.getUsername();
            String password = loginPayload.getPassword();
            
            User user = storage.getUserByUsername(username);
            if (user == null) {
                return Message.unauthorized("User not found");
            }
            
            if (!SecurityUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash())) {
                return Message.unauthorized("Invalid password");
            }
            
            userSessions.put(clientId, user.getId());
            clientToUser.put(clientId, user.getId());
            return Message.success("Login successful", user.getId());
        }

        private Message handleLogout() {
            userSessions.remove(clientId);
            clientToUser.remove(clientId);
            return Message.success("Logged out successfully", null);
        }

        private Message handleCreateBoard(Object payload) {
            String userId = userSessions.get(clientId);
            if (userId == null) {
                return Message.unauthorized("Not logged in");
            }
            
            Gson gson = GsonUtil.createGson();
            CreateBoardPayload boardPayload = gson.fromJson(gson.toJson(payload), CreateBoardPayload.class);
            String boardName = boardPayload.getBoardName();
            
            Board board = new Board(boardName, userId);
            storage.addBoard(board);
            
            User user = storage.getUserById(userId);
            user.addOwnedBoard(board.getId());
            storage.addUser(user);
            
            return Message.success("Board created successfully", board.getId());
        }

        private Message handleListBoards() {
            String userId = userSessions.get(clientId);
            if (userId == null) {
                return Message.unauthorized("Not logged in");
            }
            
            List<Board> boards = storage.getBoardsForUser(userId);
            return Message.success("Boards retrieved successfully", boards);
        }

        private Message handleAddUserToBoard(Object payload) {
            String userId = userSessions.get(clientId);
            if (userId == null) {
                return Message.unauthorized("Not logged in");
            }
            
            Gson gson = GsonUtil.createGson();
            JsonObject jsonPayload = gson.fromJson(gson.toJson(payload), JsonObject.class);
            String boardId = jsonPayload.get("boardId").getAsString();
            String targetUserId = jsonPayload.get("userId").getAsString();
            
            Board board = storage.getBoardById(boardId);
            if (board == null) {
                return Message.error("Board not found");
            }
            
            if (!board.isOwner(userId)) {
                return Message.error("You don't own this board");
            }
            
            User targetUser = storage.getUserById(targetUserId);
            if (targetUser == null) {
                return Message.error("Target user not found");
            }
            
            board.addMember(targetUserId);
            targetUser.addMemberBoard(boardId);
            storage.addBoard(board);
            storage.addUser(targetUser);
            
            return Message.success("User added to board successfully", null);
        }

        private Message handleViewBoard(Object payload) {
            String userId = userSessions.get(clientId);
            if (userId == null) {
                return Message.unauthorized("Not logged in");
            }
            
            String boardId = payload.toString().trim();
            Board board = storage.getBoardById(boardId);
            if (board == null) {
                return Message.error("Board not found");
            }
            
            if (!board.isMember(userId)) {
                return Message.error("Access denied");
            }
            
            // Store the current board for this client
            clientCurrentBoard.put(clientId, boardId);
            
            return Message.success("Entering board view mode for: " + board.getName(), board);
        }

        private Message handleAddTask(Object payload) {
            String userId = userSessions.get(clientId);
            if (userId == null) {
                return Message.unauthorized("Not logged in");
            }
            
            // Check if client is in board view mode
            String currentBoardId = clientCurrentBoard.get(clientId);
            if (currentBoardId == null) {
                return Message.error("You must be in board view mode. Use 'view_board <boardID>' first.");
            }
            
            Gson gson = GsonUtil.createGson();
            AddTaskPayload taskPayload = gson.fromJson(gson.toJson(payload), AddTaskPayload.class);
            
            String title = taskPayload.getTitle();
            String description = taskPayload.getDescription();
            String priorityStr = taskPayload.getPriority().toUpperCase();
            
            Task.Priority priority;
            try {
                priority = Task.Priority.valueOf(priorityStr);
            } catch (IllegalArgumentException e) {
                return Message.error("Invalid priority. Use: LOW, MEDIUM, HIGH");
            }
            
            Board currentBoard = storage.getBoardById(currentBoardId);
            if (currentBoard == null) {
                return Message.error("Current board not found");
            }
            
            Task task = new Task(title, description, priority, currentBoard.getId());
            storage.addTask(task);
            
            // Notify other board members
            notifyBoardMembers(currentBoard.getId(), 
                "New task added: " + title + " (Priority: " + priority + ")", userId);
            
            return Message.success("Task created successfully", task.getId());
        }

        private Message handleListTasks() {
            String userId = userSessions.get(clientId);
            if (userId == null) {
                return Message.unauthorized("Not logged in");
            }
            
            // Check if client is in board view mode
            String currentBoardId = clientCurrentBoard.get(clientId);
            if (currentBoardId == null) {
                return Message.error("You must be in board view mode. Use 'view_board <boardID>' first.");
            }
            
            List<Task> tasks = storage.getTasksForBoard(currentBoardId);
            return Message.success("Tasks retrieved successfully", tasks);
        }

        private Message handleUpdateTaskStatus(Object payload) {
            String userId = userSessions.get(clientId);
            if (userId == null) {
                return Message.unauthorized("Not logged in");
            }
            
            Gson gson = GsonUtil.createGson();
            JsonObject jsonPayload = gson.fromJson(gson.toJson(payload), JsonObject.class);
            String taskId = jsonPayload.get("taskId").getAsString();
            String statusStr = jsonPayload.get("status").getAsString().toUpperCase();
            
            Task.Status status;
            try {
                status = Task.Status.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                return Message.error("Invalid status. Use: TODO, IN_PROGRESS, DONE");
            }
            
            Task task = storage.getTaskById(taskId);
            if (task == null) {
                return Message.error("Task not found");
            }
            
            task.setStatus(status);
            storage.updateTask(task);
            
            // Notify other board members
            notifyBoardMembers(task.getBoardId(), 
                "Task status updated: " + task.getTitle() + " -> " + status, userId);
            
            return Message.success("Task status updated successfully", null);
        }

        private Message handleDeleteTask(Object payload) {
            String userId = userSessions.get(clientId);
            if (userId == null) {
                return Message.unauthorized("Not logged in");
            }
            
            String taskId = payload.toString().trim();
            Task task = storage.getTaskById(taskId);
            if (task == null) {
                return Message.error("Task not found");
            }
            
            String taskTitle = task.getTitle();
            String boardId = task.getBoardId();
            storage.deleteTask(taskId);
            
            // Notify other board members
            notifyBoardMembers(boardId, 
                "Task deleted: " + taskTitle, userId);
            
            return Message.success("Task deleted successfully", null);
        }
    }

    private void notifyBoardMembers(String boardId, String message, String excludeUserId) {
        Board board = storage.getBoardById(boardId);
        if (board == null) return;

        for (String memberId : board.getMemberIds()) {
            if (memberId.equals(excludeUserId)) continue; // Don't notify the user who made the change
            
            // Find client connected to this user
            for (Map.Entry<String, String> entry : clientToUser.entrySet()) {
                if (entry.getValue().equals(memberId)) {
                    String clientId = entry.getKey();
                    PrintWriter clientOut = connectedClients.get(clientId);
                    if (clientOut != null) {
                        Message notification = Message.success(message, null);
                        clientOut.println(notification.toJson());
                    }
                    break;
                }
            }
        }
    }

    public void stop() {
        running = false;
        threadPool.shutdown();
        System.out.println("Server stopped");
    }
}
