package com.todo.client;

import com.todo.protocol.Message;
import com.todo.protocol.LoginPayload;
import com.todo.protocol.CreateBoardPayload;
import com.todo.protocol.AddTaskPayload;
import com.todo.model.Board;
import com.todo.model.Task;
import com.todo.util.GsonUtil;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int TCP_PORT = 9999;
    private static final int UDP_PORT = 9998;
    
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
            
            if ("notification".equals(response.getStatus())) {
                handleNotification(response);
            } else if ("success".equals(response.getStatus())) {
                System.out.println("✓ " + response.getMessage());
                
                // Format data display based on content
                if (response.getData() != null) {
                    formatDataDisplay(response.getData());
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
    
    private void handleNotification(Message notification) {
        String notificationType = notification.getMessage();
        Object data = notification.getData();
        
        System.out.println("\n🔔 === REAL-TIME NOTIFICATION ===");
        
        try {
            switch (notificationType) {
                case "task_added":
                    if (data instanceof String) {
                        // Try to deserialize from JSON string
                        com.todo.model.Task task = GsonUtil.createGson().fromJson((String) data, com.todo.model.Task.class);
                        System.out.println("📝 New task added: " + task.getTitle() + " (" + task.getPriority() + ")");
                        System.out.println("   Description: " + task.getDescription());
                    } else if (data instanceof com.todo.model.Task) {
                        com.todo.model.Task task = (com.todo.model.Task) data;
                        System.out.println("📝 New task added: " + task.getTitle() + " (" + task.getPriority() + ")");
                        System.out.println("   Description: " + task.getDescription());
                    }
                    break;
                    
                case "task_updated":
                    if (data instanceof String) {
                        com.todo.model.Task task = GsonUtil.createGson().fromJson((String) data, com.todo.model.Task.class);
                        System.out.println("📝 Task updated: " + task.getTitle() + " -> " + task.getStatus());
                    } else if (data instanceof com.todo.model.Task) {
                        com.todo.model.Task task = (com.todo.model.Task) data;
                        System.out.println("📝 Task updated: " + task.getTitle() + " -> " + task.getStatus());
                    }
                    break;
                    
                case "task_deleted":
                    System.out.println("🗑️  Task deleted: " + data);
                    break;
                    
                case "user_added":
                    System.out.println("👤 User added to board: " + data);
                    break;
                    
                case "board_created":
                    if (data instanceof String) {
                        com.todo.model.Board board = GsonUtil.createGson().fromJson((String) data, com.todo.model.Board.class);
                        System.out.println("📋 New board created: " + board.getName());
                    } else if (data instanceof com.todo.model.Board) {
                        com.todo.model.Board board = (com.todo.model.Board) data;
                        System.out.println("📋 New board created: " + board.getName());
                    }
                    break;
                    
                case "user_added_to_board":
                    if (data instanceof String) {
                        String username = (String) data;
                        System.out.println("🎉 You have been added to a board by: " + username);
                    }
                    break;
                    
                default:
                    System.out.println("🔔 Notification: " + notificationType + " - " + data);
            }
        } catch (Exception e) {
            System.out.println("🔔 Notification: " + notificationType + " - " + data);
        }
        
        System.out.println("================================\n");
        System.out.print("> "); // Re-display prompt
    }
    
    private void formatDataDisplay(Object data) {
        if (data == null) {
            return;
        }
        
        Gson gson = GsonUtil.createGson();
        
        try {
            // Handle List<Board> (for list_boards command)
            if (data instanceof List) {
                List<?> dataList = (List<?>) data;
                if (!dataList.isEmpty() && dataList.get(0) instanceof com.google.gson.JsonObject) {
                    // Convert JsonObject list to Board list
                    List<Board> boards = new ArrayList<>();
                    for (Object item : dataList) {
                        Board board = gson.fromJson(gson.toJson(item), Board.class);
                        boards.add(board);
                    }
                    displayBoards(boards);
                    return;
                }
            }
            
            // Handle single Board (for view_board command)
            if (data instanceof com.google.gson.JsonObject) {
                Board board = gson.fromJson(gson.toJson(data), Board.class);
                displaySingleBoard(board);
                return;
            }
            
            // Handle String data (JSON string)
            if (data instanceof String) {
                String dataStr = (String) data;
                
                // Try to deserialize as List<Board>
                if (dataStr.startsWith("[") && (dataStr.contains("board") || dataStr.contains("ownerId"))) {
                    List<Board> boards = gson.fromJson(dataStr, new TypeToken<List<Board>>(){}.getType());
                    displayBoards(boards);
                    return;
                }
                
                // Try to deserialize as List<Task>
                if (dataStr.startsWith("[") && (dataStr.contains("task") || dataStr.contains("title"))) {
                    List<Task> tasks = gson.fromJson(dataStr, new TypeToken<List<Task>>(){}.getType());
                    displayTasks(tasks);
                    return;
                }
                
                // Try to deserialize as single Board
                if (dataStr.startsWith("{") && (dataStr.contains("board") || dataStr.contains("ownerId"))) {
                    Board board = gson.fromJson(dataStr, Board.class);
                    displaySingleBoard(board);
                    return;
                }
                
                // Try to deserialize as single Task
                if (dataStr.startsWith("{") && (dataStr.contains("task") || dataStr.contains("title"))) {
                    Task task = gson.fromJson(dataStr, Task.class);
                    displaySingleTask(task);
                    return;
                }
                
                // Simple string data (like user ID or board ID)
                System.out.println("ID: " + data);
                return;
            }
            
            // Fallback: try to convert to JSON string and parse
            String jsonStr = gson.toJson(data);
            if (jsonStr.startsWith("[") && (jsonStr.contains("board") || jsonStr.contains("ownerId"))) {
                List<Board> boards = gson.fromJson(jsonStr, new TypeToken<List<Board>>(){}.getType());
                displayBoards(boards);
                return;
            }
            
            if (jsonStr.startsWith("{") && (jsonStr.contains("board") || jsonStr.contains("ownerId"))) {
                Board board = gson.fromJson(jsonStr, Board.class);
                displaySingleBoard(board);
                return;
            }
            
            // Final fallback
            System.out.println("Data: " + data);
            
        } catch (Exception e) {
            // Fallback to simple string display if deserialization fails
            System.out.println("Data: " + data);
        }
    }
    
    
    private void displayBoards(List<Board> boards) {
        if (boards == null || boards.isEmpty()) {
            System.out.println("\n📋 No boards found");
            System.out.println("💡 Use 'create_board <name>' to create your first board");
            return;
        }
        
        System.out.println("\n📋 Your Boards:");
        System.out.println("ID\t\t\tName");
        System.out.println("─────────────────────────────────────────────────────────");
        
        for (Board board : boards) {
            System.out.println(board.getId() + "\t" + board.getName());
        }
        
        System.out.println("\n💡 Use 'view_board <ID>' to enter a board");
    }
    
    private void displaySingleBoard(Board board) {
        if (board == null) {
            System.out.println("Board not found");
            return;
        }
        
        System.out.println("\n📋 Board Details:");
        System.out.println("ID:\t\t" + board.getId());
        System.out.println("Name:\t\t" + board.getName());
        System.out.println("Created:\t" + board.getCreatedAt().toLocalDate());
        System.out.println("Members:\t" + board.getMemberIds().size());
    }
    
    private void displayTasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            System.out.println("\n📝 No tasks found in this board");
            System.out.println("💡 Use 'add_task <title> <description> <priority>' to create your first task");
            return;
        }
        
        System.out.println("\n📝 Tasks in Current Board:");
        System.out.println("ID\t\t\tTitle\t\t\tPriority\tStatus\t\tCreated");
        System.out.println("─────────────────────────────────────────────────────────────────────────────────");
        
        for (Task task : tasks) {
            String createdDate = task.getCreatedAt().toLocalDate().toString();
            System.out.println(task.getId() + "\t" + 
                truncateString(task.getTitle(), 20) + "\t" + 
                task.getPriority() + "\t\t" + 
                task.getStatus() + "\t\t" + 
                createdDate);
        }
        
        System.out.println("\n💡 Use 'update_task_status <ID> <STATUS>' to update task status");
        System.out.println("💡 Use 'delete_task <ID>' to delete a task");
    }
    
    private void displaySingleTask(Task task) {
        if (task == null) {
            System.out.println("Task not found");
            return;
        }
        
        System.out.println("\n📝 Task Details:");
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.printf("│ %-15s │ %-40s │%n", "ID:", task.getId());
        System.out.printf("│ %-15s │ %-40s │%n", "Title:", task.getTitle());
        System.out.printf("│ %-15s │ %-40s │%n", "Description:", truncateString(task.getDescription(), 40));
        System.out.printf("│ %-15s │ %-40s │%n", "Priority:", task.getPriority());
        System.out.printf("│ %-15s │ %-40s │%n", "Status:", task.getStatus());
        System.out.printf("│ %-15s │ %-40s │%n", "Created:", task.getCreatedAt().toLocalDate());
        System.out.println("└─────────────────────────────────────────────────────────┘");
    }
    
    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private void printHelp() {
        System.out.println("\n🚀 === Todo List Client Commands === 🚀");
        System.out.println();
        System.out.println("🔐 Authentication:");
        System.out.println("  register <username> <password>  - Register a new user");
        System.out.println("  login <username> <password>     - Login to your account");
        System.out.println("  logout                          - Logout from current account");
        System.out.println();
        System.out.println("📋 Board Management:");
        System.out.println("  create_board <boardName>        - Create a new board");
        System.out.println("  list_boards                     - List all your boards (formatted table)");
        System.out.println("  add_user_to_board <boardID> <userID> - Add user to board");
        System.out.println("  view_board <boardID>            - Enter board view mode");
        System.out.println();
        System.out.println("📝 Task Management (only in board view mode):");
        System.out.println("  add_task <title> <description> <priority> - Add new task");
        System.out.println("  list_tasks                      - List all tasks (formatted table)");
        System.out.println("  update_task_status <taskID> <status> - Update task status");
        System.out.println("  delete_task <taskID>            - Delete a task");
        System.out.println();
        System.out.println("❓ Other:");
        System.out.println("  help                            - Show this help");
        System.out.println("  exit/quit                       - Exit the client");
        System.out.println();
        System.out.println("📊 Values:");
        System.out.println("  Priority: LOW, MEDIUM, HIGH");
        System.out.println("  Status: TODO, IN_PROGRESS, DONE");
        System.out.println();
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
