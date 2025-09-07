package com.todo.client;

import com.todo.protocol.Message;
import com.todo.protocol.LoginPayload;
import com.todo.protocol.CreateBoardPayload;
import com.todo.protocol.AddTaskPayload;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int TCP_PORT = 8080;
    private static final int UDP_PORT = 8081;
    
    private Socket tcpSocket;
    private PrintWriter tcpOut;
    private BufferedReader tcpIn;
    private DatagramSocket udpSocket;
    private InetAddress serverAddress;
    
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean inBoardView = new AtomicBoolean(false);
    private String currentBoardId;
    private Scanner scanner;

    public Client() {
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            connectToServer();
            startListenerThread();
            startCommandLoop();
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void connectToServer() throws IOException {
        tcpSocket = new Socket(SERVER_HOST, TCP_PORT);
        tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
        tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
        
        udpSocket = new DatagramSocket();
        serverAddress = InetAddress.getByName(SERVER_HOST);
        
        connected.set(true);
        System.out.println("Connected to server at " + SERVER_HOST + ":" + TCP_PORT);
        printHelp();
    }

    private void startListenerThread() {
        Thread listenerThread = new Thread(() -> {
            try {
                String response;
                while (connected.get() && (response = tcpIn.readLine()) != null) {
                    handleServerResponse(response);
                }
            } catch (IOException e) {
                if (connected.get()) {
                    System.err.println("Error reading from server: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void startCommandLoop() {
        System.out.println("\nEnter commands (type 'help' for available commands):");
        
        while (connected.get()) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) continue;
            
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                break;
            }
            
            if (input.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }
            
            processCommand(input);
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split(" ", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "register":
                handleRegister(args);
                break;
            case "login":
                handleLogin(args);
                break;
            case "logout":
                sendMessage("logout", null);
                break;
            case "create_board":
                handleCreateBoard(args);
                break;
            case "list_boards":
                sendMessage("list_boards", null);
                break;
            case "add_user_to_board":
                handleAddUserToBoard(args);
                break;
            case "view_board":
                sendMessage("view_board", args);
                break;
            case "add_task":
                if (inBoardView.get()) {
                    handleAddTask(args);
                } else {
                    System.out.println("Error: You must be in board view mode. Use 'view_board <boardID>' first.");
                }
                break;
            case "list_tasks":
                if (inBoardView.get()) {
                    sendMessage("list_tasks", null);
                } else {
                    System.out.println("Error: You must be in board view mode. Use 'view_board <boardID>' first.");
                }
                break;
            case "update_task_status":
                if (inBoardView.get()) {
                    handleUpdateTaskStatus(args);
                } else {
                    System.out.println("Error: You must be in board view mode. Use 'view_board <boardID>' first.");
                }
                break;
            case "delete_task":
                if (inBoardView.get()) {
                    sendMessage("delete_task", args);
                } else {
                    System.out.println("Error: You must be in board view mode. Use 'view_board <boardID>' first.");
                }
                break;
            default:
                System.out.println("Unknown command: " + command);
                printHelp();
        }
    }

    private void sendMessage(String command, Object payload) {
        if (!connected.get()) {
            System.out.println("Not connected to server");
            return;
        }
        
        Message message = new Message(command, payload);
        tcpOut.println(message.toJson());
    }

    private void handleRegister(String args) {
        String[] parts = args.split(" ");
        if (parts.length != 2) {
            System.out.println("Usage: register <username> <password>");
            return;
        }
        LoginPayload payload = new LoginPayload(parts[0], parts[1]);
        sendMessage("register", payload);
    }

    private void handleLogin(String args) {
        String[] parts = args.split(" ");
        if (parts.length != 2) {
            System.out.println("Usage: login <username> <password>");
            return;
        }
        LoginPayload payload = new LoginPayload(parts[0], parts[1]);
        sendMessage("login", payload);
    }

    private void handleCreateBoard(String args) {
        if (args.trim().isEmpty()) {
            System.out.println("Usage: create_board <boardName>");
            return;
        }
        CreateBoardPayload payload = new CreateBoardPayload(args.trim());
        sendMessage("create_board", payload);
    }

    private void handleAddUserToBoard(String args) {
        String[] parts = args.split(" ");
        if (parts.length != 2) {
            System.out.println("Usage: add_user_to_board <boardID> <userID>");
            return;
        }
        // Simple payload for this command
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("boardId", parts[0]);
        payload.put("userId", parts[1]);
        sendMessage("add_user_to_board", payload);
    }

    private void handleAddTask(String args) {
        String[] parts = args.split(" ", 3);
        if (parts.length != 3) {
            System.out.println("Usage: add_task <title> <description> <priority>");
            return;
        }
        AddTaskPayload payload = new AddTaskPayload(parts[0], parts[1], parts[2]);
        sendMessage("add_task", payload);
    }

    private void handleUpdateTaskStatus(String args) {
        String[] parts = args.split(" ");
        if (parts.length != 2) {
            System.out.println("Usage: update_task_status <taskID> <status>");
            return;
        }
        java.util.Map<String, String> payload = new java.util.HashMap<>();
        payload.put("taskId", parts[0]);
        payload.put("status", parts[1]);
        sendMessage("update_task_status", payload);
    }

    private void handleServerResponse(String responseJson) {
        try {
            Message response = Message.fromJson(responseJson);
            
            if ("success".equals(response.getStatus())) {
                System.out.println("✓ " + response.getMessage());
                if (response.getData() != null) {
                    System.out.println("Data: " + response.getData());
                }
                
                // Handle view_board success - set board view mode
                if (response.getMessage().contains("Entering board view mode")) {
                    inBoardView.set(true);
                    // Extract board ID from the data if it's a Board object
                    if (response.getData() instanceof com.todo.model.Board) {
                        com.todo.model.Board board = (com.todo.model.Board) response.getData();
                        currentBoardId = board.getId();
                    }
                }
            } else if ("unauthorized".equals(response.getStatus())) {
                System.out.println("✗ Unauthorized: " + response.getMessage());
            } else {
                System.out.println("✗ Error: " + response.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error parsing server response: " + e.getMessage());
        }
    }

    private void printHelp() {
        System.out.println("\n=== Todo List Client Commands ===");
        System.out.println("Authentication:");
        System.out.println("  register <username> <password>  - Register a new user");
        System.out.println("  login <username> <password>     - Login to your account");
        System.out.println("  logout                          - Logout from current account");
        System.out.println();
        System.out.println("Board Management:");
        System.out.println("  create_board <boardName>        - Create a new board");
        System.out.println("  list_boards                     - List all your boards");
        System.out.println("  add_user_to_board <boardID> <userID> - Add user to board");
        System.out.println("  view_board <boardID>            - Enter board view mode");
        System.out.println();
        System.out.println("Task Management (only in board view mode):");
        System.out.println("  add_task <title> <description> <priority> - Add new task");
        System.out.println("  list_tasks                      - List all tasks in current board");
        System.out.println("  update_task_status <taskID> <status> - Update task status");
        System.out.println("  delete_task <taskID>            - Delete a task");
        System.out.println();
        System.out.println("Other:");
        System.out.println("  help                            - Show this help");
        System.out.println("  exit/quit                       - Exit the client");
        System.out.println();
        System.out.println("Priority values: LOW, MEDIUM, HIGH");
        System.out.println("Status values: TODO, IN_PROGRESS, DONE");
        System.out.println("=====================================\n");
    }

    private void disconnect() {
        connected.set(false);
        try {
            if (tcpSocket != null) tcpSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing connections: " + e.getMessage());
        }
        System.out.println("Disconnected from server");
    }
}
