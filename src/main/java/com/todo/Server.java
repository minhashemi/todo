package com.todo;

import com.todo.model.Task;
import com.todo.service.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

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
    private final UserService userService = new UserService();           // Handles user operations
    private final BoardService boardService = new BoardService();        // Handles board/task operations
    private final NotificationService notificationService;               // Handles real-time notifications
    
    // Thread-safe data structures for concurrent access
    private final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();      // clientId -> output stream
    private final Map<String, String> sessions = new ConcurrentHashMap<>();          // clientId -> username
    private final Map<String, String> currentBoard = new ConcurrentHashMap<>();      // clientId -> boardId
    
    // Thread pool for handling multiple clients
    private final ExecutorService pool = Executors.newCachedThreadPool();

    /**
     * Constructor initializes the notification service
     */
    public Server() {
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
     * Receives and logs UDP messages (currently just for demonstration)
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
     * Processes incoming commands from clients
     * Routes commands to appropriate handler methods
     * @param input - raw command string from client
     * @param clientId - ID of the client sending the command
     * @return response string to send back to client
     */
    private String processCommand(String input, String clientId) {
        String[] parts = input.split(" ", 3);  // Split into command and arguments
        String cmd = parts[0];
        
        // Route command to appropriate handler method
        return switch (cmd) {
            case "register" -> handleRegister(parts[1], parts[2]);
            case "login" -> handleLogin(clientId, parts[1], parts[2]);
            case "logout" -> handleLogout(clientId);
            case "create_board" -> handleCreateBoard(clientId, parts[1]);
            case "list_boards" -> handleListBoards(clientId);
            case "add_user_to_board" -> handleAddUserToBoard(clientId, parts[1], parts[2]);
            case "view_board" -> handleViewBoard(clientId, parts[1]);
            case "add_task" -> handleAddTask(clientId, parts[1], parts[2]);
            case "list_tasks" -> handleListTasks(clientId);
            case "update_task_status" -> handleUpdateTaskStatus(clientId, parts[1], parts[2]);
            case "delete_task" -> handleDeleteTask(clientId, parts[1]);
            default -> "ERROR: Unknown command";
        };
    }

    // ==================== USER AUTHENTICATION HANDLERS ====================
    
    /**
     * Handles user registration command
     * @param username - username to register
     * @param password - password for the user
     * @return success or error message
     */
    private String handleRegister(String username, String password) {
        return userService.register(username, password) ? 
            "SUCCESS: Registered" : "ERROR: User exists";
    }

    /**
     * Handles user login command
     * @param clientId - ID of the client logging in
     * @param username - username to login
     * @param password - password to verify
     * @return success or error message
     */
    private String handleLogin(String clientId, String username, String password) {
        if (userService.login(username, password)) {
            sessions.put(clientId, username);  // Store session
            return "SUCCESS: Logged in";
        }
        return "ERROR: Invalid credentials";
    }

    /**
     * Handles user logout command
     * @param clientId - ID of the client logging out
     * @return success message
     */
    private String handleLogout(String clientId) {
        sessions.remove(clientId);      // Remove session
        currentBoard.remove(clientId);  // Clear current board
        return "SUCCESS: Logged out";
    }

    // ==================== BOARD MANAGEMENT HANDLERS ====================
    
    /**
     * Handles board creation command
     * @param clientId - ID of the client creating the board
     * @param name - name of the board to create
     * @return success or error message
     */
    private String handleCreateBoard(String clientId, String name) {
        String user = sessions.get(clientId);
        if (user == null) return "ERROR: Not logged in";
        
        String boardId = boardService.createBoard(name, user);
        notificationService.notifyBoardCreated(boardId);  // Notify all clients
        return "SUCCESS: Board created " + boardId;
    }

    /**
     * Handles board listing command
     * @param clientId - ID of the client requesting boards
     * @return list of accessible boards or error message
     */
    private String handleListBoards(String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return "ERROR: Not logged in";
        
        List<String> boards = boardService.getUserBoards(user);
        return boards.isEmpty() ? "No boards" : String.join(",", boards);
    }

    /**
     * Handles adding user to board command
     * @param clientId - ID of the client making the request
     * @param boardId - ID of the board to add user to
     * @param username - username to add to the board
     * @return success or error message
     */
    private String handleAddUserToBoard(String clientId, String boardId, String username) {
        String user = sessions.get(clientId);
        if (user == null) return "ERROR: Not logged in";
        if (!userService.userExists(username)) return "ERROR: User not found";
        
        if (boardService.addUserToBoard(boardId, username, user)) {
            notificationService.notifyUserAdded(username);  // Notify all clients
            return "SUCCESS: User added";
        }
        return "ERROR: Access denied";
    }

    /**
     * Handles board viewing command (enters board view mode)
     * @param clientId - ID of the client viewing the board
     * @param boardId - ID of the board to view
     * @return success or error message
     */
    private String handleViewBoard(String clientId, String boardId) {
        String user = sessions.get(clientId);
        if (user == null) return "ERROR: Not logged in";
        
        if (boardService.canAccessBoard(boardId, user)) {
            currentBoard.put(clientId, boardId);  // Set current board for client
            String boardName = boardService.getBoardName(boardId);
            return "SUCCESS: Viewing board " + boardName;
        }
        return "ERROR: Access denied";
    }

    // ==================== TASK MANAGEMENT HANDLERS ====================
    
    /**
     * Handles task creation command (requires board view mode)
     * @param clientId - ID of the client creating the task
     * @param title - title of the task
     * @param desc - description of the task
     * @return success or error message
     */
    private String handleAddTask(String clientId, String title, String desc) {
        String user = sessions.get(clientId);
        if (user == null) return "ERROR: Not logged in";
        
        String boardId = currentBoard.get(clientId);
        if (boardId == null) return "ERROR: Not in board view";
        
        String taskId = "t" + System.currentTimeMillis();  // Generate unique task ID
        Task task = new Task(taskId, title, desc);
        boardService.addTask(boardId, task);
        notificationService.notifyTaskAdded(taskId, title);  // Notify all clients
        return "SUCCESS: Task added " + taskId;
    }

    /**
     * Handles task listing command (requires board view mode)
     * @param clientId - ID of the client requesting tasks
     * @return list of tasks or error message
     */
    private String handleListTasks(String clientId) {
        String user = sessions.get(clientId);
        if (user == null) return "ERROR: Not logged in";
        
        String boardId = currentBoard.get(clientId);
        if (boardId == null) return "ERROR: Not in board view";
        
        List<Task> tasks = boardService.getTasks(boardId);
        return tasks.isEmpty() ? "No tasks" : 
            tasks.stream().map(Task::toString).reduce((a, b) -> a + "," + b).orElse("");
    }

    /**
     * Handles task status update command (requires board view mode)
     * @param clientId - ID of the client updating the task
     * @param taskId - ID of the task to update
     * @param status - new status (TODO, IN_PROGRESS, DONE)
     * @return success or error message
     */
    private String handleUpdateTaskStatus(String clientId, String taskId, String status) {
        String user = sessions.get(clientId);
        if (user == null) return "ERROR: Not logged in";
        
        String boardId = currentBoard.get(clientId);
        if (boardId == null) return "ERROR: Not in board view";
        
        if (boardService.updateTaskStatus(boardId, taskId, status)) {
            notificationService.notifyTaskUpdated(taskId, status);  // Notify all clients
            return "SUCCESS: Task updated";
        }
        return "ERROR: Task not found";
    }

    /**
     * Handles task deletion command (requires board view mode)
     * @param clientId - ID of the client deleting the task
     * @param taskId - ID of the task to delete
     * @return success or error message
     */
    private String handleDeleteTask(String clientId, String taskId) {
        String user = sessions.get(clientId);
        if (user == null) return "ERROR: Not logged in";
        
        String boardId = currentBoard.get(clientId);
        if (boardId == null) return "ERROR: Not in board view";
        
        if (boardService.deleteTask(boardId, taskId)) {
            notificationService.notifyTaskDeleted(taskId);  // Notify all clients
            return "SUCCESS: Task deleted";
        }
        return "ERROR: Task not found";
    }
}
