package com.todo;

import com.todo.protocol.Message;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client class for the Todo List Application
 * Handles user input, server communication, and real-time notifications
 * Uses dual-thread architecture: main thread for input, listener thread for responses
 */
public class Client {
    // Network configuration
    private static final String HOST = "localhost";  // Server hostname
    private static final int TCP_PORT = 1234;        // Port for command communication
    private static final int UDP_PORT = 4321;        // Port for notifications
    
    // Network connections
    private Socket tcp;                              // TCP socket for commands
    private PrintWriter out;                         // Output stream to server
    private BufferedReader in;                       // Input stream from server
    private DatagramSocket udp;                      // UDP socket for notifications
    
    // Thread-safe state management
    private AtomicBoolean connected = new AtomicBoolean(false);  // Connection status
    private AtomicBoolean inBoard = new AtomicBoolean(false);   // Board view mode status
    
    // User input
    private Scanner scanner = new Scanner(System.in);

    /**
     * Main entry point - starts the client
     * @param args - command line arguments (not used)
     */
    public static void main(String[] args) {
        new Client().start();
    }

    /**
     * Starts the client by connecting to server and starting command loop
     * Handles connection errors gracefully
     */
    public void start() {
        try {
            connect();           // Connect to server
            startListener();     // Start listener thread for responses
            commandLoop();       // Start main command loop
        } catch (Exception e) { 
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace(); 
        }
        finally { 
            disconnect();  // Clean up connections
        }
    }

    /**
     * Establishes connection to the server
     * Sets up TCP and UDP sockets for communication
     * @throws Exception if connection fails
     */
    private void connect() throws Exception {
        tcp = new Socket(HOST, TCP_PORT);  // Connect to server
        out = new PrintWriter(tcp.getOutputStream(), true);  // Output stream
        in = new BufferedReader(new InputStreamReader(tcp.getInputStream()));  // Input stream
        udp = new DatagramSocket();  // UDP socket for notifications
        connected.set(true);  // Mark as connected
        System.out.println("Connected! Commands: register, login, create_board, list_boards, view_board, add_task, list_tasks, update_task_status, delete_task, logout, exit");
    }

    /**
     * Starts the listener thread for receiving server responses and notifications
     * Runs in background to handle real-time updates
     */
    private void startListener() {
        new Thread(() -> {
            try {
                String line;
                while (connected.get() && (line = in.readLine()) != null) {
                    if (line.startsWith("NOTIFY:")) {
                        // Handle real-time notifications
                        String[] parts = line.split(":", 3);
                        System.out.println("🔔 " + parts[1] + ": " + parts[2]);
                    } else {
                        // Handle JSON responses
                        try {
                            Message response = Message.fromJson(line);
                            if ("success".equals(response.getStatus())) {
                                System.out.println("✅ " + response.getMessage());
                                if (response.getData() != null) {
                                    System.out.println("📊 " + response.getData());
                                }
                            } else if ("error".equals(response.getStatus())) {
                                System.out.println("❌ " + response.getMessage());
                            }
                        } catch (Exception e) {
                            System.out.println("📋 " + line);
                        }
                    }
                }
            } catch (Exception e) { 
                if (connected.get()) {
                    System.err.println("Listener error: " + e.getMessage());
                    e.printStackTrace(); 
                }
            }
        }).start();
    }

    /**
     * Main command loop - reads user input and processes commands
     * Runs on main thread for user interaction
     */
    private void commandLoop() {
        while (connected.get()) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;  // Skip empty input
            if (input.equals("exit")) break;  // Exit command
            
            processCommand(input);  // Process the command
        }
    }

    /**
     * Processes user commands and sends them to server as JSON
     * Handles command validation and board view mode requirements
     * @param input - user input command
     */
    private void processCommand(String input) {
        String[] parts = input.split(" ", 3);  // Split into command and arguments
        String cmd = parts[0];
        
        switch (cmd) {
            // User authentication commands
            case "register" -> {
                Map<String, String> payload = new HashMap<>();
                payload.put("username", parts[1]);
                payload.put("password", parts[2]);
                send(new Message("register", payload).toJson());
            }
            case "login" -> {
                Map<String, String> payload = new HashMap<>();
                payload.put("username", parts[1]);
                payload.put("password", parts[2]);
                send(new Message("login", payload).toJson());
            }
            case "logout" -> { 
                send(new Message("logout", null).toJson()); 
                inBoard.set(false);  // Exit board view mode
            }
            
            // Board management commands
            case "create_board" -> {
                Map<String, String> payload = new HashMap<>();
                payload.put("name", parts[1]);
                send(new Message("create_board", payload).toJson());
            }
            case "list_boards" -> send(new Message("list_boards", null).toJson());
            case "add_user_to_board" -> {
                Map<String, String> payload = new HashMap<>();
                payload.put("boardId", parts[1]);
                payload.put("username", parts[2]);
                send(new Message("add_user_to_board", payload).toJson());
            }
            case "view_board" -> { 
                Map<String, String> payload = new HashMap<>();
                payload.put("boardId", parts[1]);
                send(new Message("view_board", payload).toJson()); 
                inBoard.set(true);  // Enter board view mode
            }
            
            // Task management commands (require board view mode)
            case "add_task" -> {
                if (inBoard.get()) {
                    Map<String, String> payload = new HashMap<>();
                    payload.put("title", parts[1]);
                    payload.put("description", parts[2]);
                    send(new Message("add_task", payload).toJson());
                } else {
                    System.out.println("❌ Must be in board view first");
                }
            }
            case "list_tasks" -> {
                if (inBoard.get()) send(new Message("list_tasks", null).toJson());
                else System.out.println("❌ Must be in board view first");
            }
            case "update_task_status" -> {
                if (inBoard.get()) {
                    Map<String, String> payload = new HashMap<>();
                    payload.put("taskId", parts[1]);
                    payload.put("status", parts[2]);
                    send(new Message("update_task_status", payload).toJson());
                } else {
                    System.out.println("❌ Must be in board view first");
                }
            }
            case "delete_task" -> {
                if (inBoard.get()) {
                    Map<String, String> payload = new HashMap<>();
                    payload.put("taskId", parts[1]);
                    send(new Message("delete_task", payload).toJson());
                } else {
                    System.out.println("❌ Must be in board view first");
                }
            }
            default -> System.out.println("❌ Unknown command");
        }
    }

    /**
     * Sends a message to the server
     * @param msg - message to send
     */
    private void send(String msg) {
        if (connected.get()) out.println(msg);
    }

    /**
     * Disconnects from the server and cleans up resources
     */
    private void disconnect() {
        connected.set(false);  // Mark as disconnected
        try { 
            if (tcp != null) tcp.close(); 
            if (udp != null) udp.close(); 
        } catch (Exception e) { 
            System.err.println("Disconnect error: " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}
