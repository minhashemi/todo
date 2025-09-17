package com.todo;

import com.todo.model.Task;
import com.todo.service.*;
import com.todo.storage.DatabaseStorage;
import com.todo.protocol.Message;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Main Server class for the Todo List Application
 * Handles client connections, command processing, and real-time notifications
 * Uses multi-threading to serve multiple clients simultaneously
 */
public class Server {
    // Network configuration
    private static final int TCP_PORT = 1234;  // Port for command communication
    private static final int UDP_PORT = 4321;  // Port for notifications
    
    // Service layer components (following Clean Code principles)
    private final DatabaseStorage database;                              // Database storage
    private final UserService userService;                              // Handles user operations
    private final BoardService boardService;                            // Handles board/task operations
    private final NotificationService notificationService;               // Handles real-time notifications
    
    // Thread-safe data structures for concurrent access
    private final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();      // clientId -> output stream
    private final Map<String, String> sessions = new ConcurrentHashMap<>();          // clientId -> username
    private final Map<String, String> currentBoard = new ConcurrentHashMap<>();      // clientId -> boardId
    
    // Thread pool for handling multiple clients
    private final ExecutorService pool = Executors.newCachedThreadPool();

    /**
     * Constructor initializes the database and services
     */
    public Server() {
        this.database = new DatabaseStorage();
        this.userService = new UserService(database);
        this.boardService = new BoardService(database);
        this.notificationService = new NotificationService(clients);
    }
    
    /**
     * Main entry point - starts the server
     * @param args - command line arguments (not used)
     */
    public static void main(String[] args) throws Exception {
        new Server().start();
    }
    
    /**
     * Starts the server by launching TCP and UDP server threads
     * @throws Exception if server startup fails
     */
    public void start() throws Exception {
        System.out.println("Starting Clean Todo Server...");
        pool.submit(this::startTcpServer);    // Start TCP server for commands
        pool.submit(this::startUdpServer);    // Start UDP server for notifications
        System.out.println("Server started on ports " + TCP_PORT + " and " + UDP_PORT);
    }

    /**
     * Starts TCP server for command communication
     * Accepts client connections and handles them in separate threads
     */
    private void startTcpServer() {
        try (ServerSocket ss = new ServerSocket(TCP_PORT)) {
            System.out.println("TCP Server listening on port " + TCP_PORT);
            while (true) {
                Socket s = ss.accept();  // Wait for client connection
                pool.submit(() -> handleClient(s));  // Handle client in separate thread
            }
        } catch (Exception e) { 
            System.err.println("TCP Server error: " + e.getMessage());
            e.printStackTrace(); 
        }
    }

    /**
     * Starts UDP server for notifications
     * Receives and logs UDP messages
     */
    private void startUdpServer() {
        try (DatagramSocket ds = new DatagramSocket(UDP_PORT)) {
            System.out.println("UDP Server listening on port " + UDP_PORT);
            byte[] buf = new byte[1024];
            while (true) {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                ds.receive(p);  // Wait for UDP packet
                System.out.println("UDP: " + new String(p.getData(), 0, p.getLength()));
            }
        } catch (Exception e) { 
            System.err.println("UDP Server error: " + e.getMessage());
            e.printStackTrace(); 
        }
    }

    /**
     * Handles a connected client in a separate thread
     * Processes commands and sends responses
     * @param s - client socket connection
     */
    private void handleClient(Socket s) {
        String clientId = "c" + System.currentTimeMillis();  // Generate unique client ID
        try (PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            
            clients.put(clientId, out);  // Register client for notifications
            System.out.println("Client connected: " + clientId);
            
            String line;
            while ((line = in.readLine()) != null) {  // Read commands from client
                String result = processCommand(line, clientId);  // Process command
                out.println(result);  // Send response back to client
            }
        } catch (Exception e) { 
            System.err.println("Client handling error: " + e.getMessage());
            e.printStackTrace(); 
        }
        finally { 
            // Clean up when client disconnects
            clients.remove(clientId); 
            sessions.remove(clientId); 
            currentBoard.remove(clientId);
            System.out.println("Client disconnected: " + clientId);
        }
    }

    /**
     * Processes incoming JSON messages from clients
     * Routes commands to appropriate handler methods
     * @param jsonInput - JSON message from client
     * @param clientId - ID of the client sending the command
     * @return JSON response to send back to client
     */
    private String processCommand(String jsonInput, String clientId) {
        try {
            Message message = Message.fromJson(jsonInput);
            String command = message.getCommand();
            Object payload = message.getPayload();
            
            // Route command to appropriate handler method
            Message response = switch (command) {
                case "register" -> handleRegister(payload, clientId);
                case "login" -> handleLogin(payload, clientId);
                case "logout" -> handleLogout(clientId);
                case "create_board" -> handleCreateBoard(payload, clientId);
                case "list_boards" -> handleListBoards(clientId);
                case "add_user_to_board" -> handleAddUserToBoard(payload, clientId);
                case "view_board" -> handleViewBoard(payload, clientId);
                case "add_task" -> handleAddTask(payload, clientId);
                case "list_tasks" -> handleListTasks(clientId);
                case "update_task_status" -> handleUpdateTaskStatus(payload, clientId);
                case "delete_task" -> handleDeleteTask(payload, clientId);
                default -> Message.error("Unknown command: " + command);
            };
            
            return response.toJson();
        } catch (Exception e) {
            return Message.error("Server error: " + e.getMessage()).toJson();
        }
    }

    // ==================== USER AUTHENTICATION HANDLERS ====================
    
    /**
     * Handles user registration command
     * @param payload - JSON payload with username and password
     * @param clientId - client ID
     * @return JSON response
     */
    private Message handleRegister(Object payload, String clientId) {
        try {
            Map<String, String> data = (Map<String, String>) payload;
            String username = data.get("username");
            String password = data.get("password");
            
            return userService.register(username, password) ? 
                Message.success("Registered successfully", null) : 
                Message.error("User already exists");
        } catch (Exception e) {
            return Message.error("Invalid registration data");
        }
    }

    /**
     * Handles user login command
     * @param payload - JSON payload with username and password
     * @param clientId - ID of the client logging in
     * @return JSON response
     */
    private Message handleLogin(Object payload, String clientId) {
        try {
            Map<String, String> data = (Map<String, String>) payload;
            String username = data.get("username");
            String password = data.get("password");
            
            if (userService.login(username, password)) {
                sessions.put(clientId, username);  // Store session
                return Message.success("Logged in successfully", null);
            }
            return Message.error("Invalid credentials");
        } catch (Exception e) {
            return Message.error("Invalid login data");
        }
    }

    /**
     * Handles user logout command
     * @param clientId - ID of the client logging out
     * @return JSON response
     */
    private Message handleLogout(String clientId) {
        sessions.remove(clientId);      // Remove session
        currentBoard.remove(clientId);  // Clear current board
        return Message.success("Logged out successfully", null);
    }

    // ==================== BOARD MANAGEMENT HANDLERS ====================
    
    /**
     * Handles board creation command
     * @param payload - JSON payload with board name
     * @param clientId - ID of the client creating the board
     * @return JSON response
     */
    private Message handleCreateBoard(Object payload, String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return Message.error("Not logged in");
        
        try {
            Map<String, String> data = (Map<String, String>) payload;
            String name = data.get("name");
            
            String boardId = boardService.createBoard(name, user);
            notificationService.notifyBoardCreated(boardId);  // Notify all clients
            return Message.success("Board created successfully", boardId);
        } catch (Exception e) {
            return Message.error("Invalid board data");
        }
    }

    /**
     * Handles board listing command
     * @param clientId - ID of the client requesting boards
     * @return JSON response with board list
     */
    private Message handleListBoards(String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return Message.error("Not logged in");
        
        List<String> boards = boardService.getUserBoards(user);
        return Message.success("Boards retrieved", boards.isEmpty() ? "No boards" : boards);
    }

    /**
     * Handles adding user to board command
     * @param payload - JSON payload with boardId and username
     * @param clientId - ID of the client making the request
     * @return JSON response
     */
    private Message handleAddUserToBoard(Object payload, String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return Message.error("Not logged in");
        
        try {
            Map<String, String> data = (Map<String, String>) payload;
            String boardId = data.get("boardId");
            String username = data.get("username");
            
            if (!userService.userExists(username)) return Message.error("User not found");
            
            if (boardService.addUserToBoard(boardId, username, user)) {
                notificationService.notifyUserAdded(username);  // Notify all clients
                return Message.success("User added to board", null);
            }
            return Message.error("Access denied");
        } catch (Exception e) {
            return Message.error("Invalid data");
        }
    }

    /**
     * Handles board viewing command (enters board view mode)
     * @param payload - JSON payload with boardId
     * @param clientId - ID of the client viewing the board
     * @return JSON response
     */
    private Message handleViewBoard(Object payload, String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return Message.error("Not logged in");
        
        try {
            Map<String, String> data = (Map<String, String>) payload;
            String boardId = data.get("boardId");
            
            if (boardService.canAccessBoard(boardId, user)) {
                currentBoard.put(clientId, boardId);  // Set current board for client
                String boardName = boardService.getBoardName(boardId);
                return Message.success("Viewing board: " + boardName, null);
            }
            return Message.error("Access denied");
        } catch (Exception e) {
            return Message.error("Invalid board data");
        }
    }

    // ==================== TASK MANAGEMENT HANDLERS ====================
    
    /**
     * Handles task creation command (requires board view mode)
     * @param payload - JSON payload with title and description
     * @param clientId - ID of the client creating the task
     * @return JSON response
     */
    private Message handleAddTask(Object payload, String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return Message.error("Not logged in");
        
        String boardId = currentBoard.get(clientId);
        if (boardId == null) return Message.error("Not in board view");
        
        try {
            Map<String, String> data = (Map<String, String>) payload;
            String title = data.get("title");
            String desc = data.get("description");
            
            String taskId = "t" + System.currentTimeMillis();  // Generate unique task ID
            Task task = new Task(taskId, title, desc);
            boardService.addTask(boardId, task);
            notificationService.notifyTaskAdded(taskId, title);  // Notify all clients
            return Message.success("Task added successfully", taskId);
        } catch (Exception e) {
            return Message.error("Invalid task data");
        }
    }

    /**
     * Handles task listing command (requires board view mode)
     * @param clientId - ID of the client requesting tasks
     * @return JSON response with task list
     */
    private Message handleListTasks(String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return Message.error("Not logged in");
        
        String boardId = currentBoard.get(clientId);
        if (boardId == null) return Message.error("Not in board view");
        
        List<Task> tasks = boardService.getTasks(boardId);
        List<String> taskStrings = tasks.stream().map(Task::toString).collect(Collectors.toList());
        return Message.success("Tasks retrieved", taskStrings.isEmpty() ? "No tasks" : taskStrings);
    }

    /**
     * Handles task status update command (requires board view mode)
     * @param payload - JSON payload with taskId and status
     * @param clientId - ID of the client updating the task
     * @return JSON response
     */
    private Message handleUpdateTaskStatus(Object payload, String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return Message.error("Not logged in");
        
        String boardId = currentBoard.get(clientId);
        if (boardId == null) return Message.error("Not in board view");
        
        try {
            Map<String, String> data = (Map<String, String>) payload;
            String taskId = data.get("taskId");
            String status = data.get("status");
            
            if (boardService.updateTaskStatus(boardId, taskId, status)) {
                notificationService.notifyTaskUpdated(taskId, status);  // Notify all clients
                return Message.success("Task updated successfully", null);
            }
            return Message.error("Task not found");
        } catch (Exception e) {
            return Message.error("Invalid task data");
        }
    }

    /**
     * Handles task deletion command (requires board view mode)
     * @param payload - JSON payload with taskId
     * @param clientId - ID of the client deleting the task
     * @return JSON response
     */
    private Message handleDeleteTask(Object payload, String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return Message.error("Not logged in");
        
        String boardId = currentBoard.get(clientId);
        if (boardId == null) return Message.error("Not in board view");
        
        try {
            Map<String, String> data = (Map<String, String>) payload;
            String taskId = data.get("taskId");
            
            if (boardService.deleteTask(boardId, taskId)) {
                notificationService.notifyTaskDeleted(taskId);  // Notify all clients
                return Message.success("Task deleted successfully", null);
            }
            return Message.error("Task not found");
        } catch (Exception e) {
            return Message.error("Invalid task data");
        }
    }
}
