package com.todo.gui;

import com.todo.model.Board;
import com.todo.model.Task;
import com.todo.protocol.Message;
import com.todo.protocol.LoginPayload;
import com.todo.protocol.CreateBoardPayload;
import com.todo.protocol.AddTaskPayload;
import com.todo.util.GsonUtil;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleClient {
    private static final String SERVER_HOST = "localhost";
    private static final int TCP_PORT = 8080;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private String currentUserId;
    private String currentUsername;
    private String currentBoardId;
    private String clientId;
    
    public SimpleClient() {
        try {
            connectToServer();
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
    
    private void connectToServer() throws IOException {
        socket = new Socket(SERVER_HOST, TCP_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected.set(true);
        clientId = socket.getLocalAddress().toString() + ":" + socket.getLocalPort();
    }
    
    public boolean register(String username, String password) {
        if (!connected.get()) return false;
        
        LoginPayload payload = new LoginPayload(username, password);
        Message message = new Message("register", payload);
        out.println(message.toJson());
        
        try {
            String response = in.readLine();
            Message responseMsg = Message.fromJson(response);
            return "success".equals(responseMsg.getStatus());
        } catch (IOException e) {
            return false;
        }
    }
    
    public boolean login(String username, String password) {
        if (!connected.get()) return false;
        
        LoginPayload payload = new LoginPayload(username, password);
        Message message = new Message("login", payload);
        out.println(message.toJson());
        
        try {
            String response = in.readLine();
            Message responseMsg = Message.fromJson(response);
            if ("success".equals(responseMsg.getStatus())) {
                currentUsername = username;
                currentUserId = responseMsg.getData().toString();
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
    
    public void logout() {
        if (!connected.get()) return;
        
        Message message = new Message("logout", null);
        out.println(message.toJson());
        currentUserId = null;
    }
    
    public boolean createBoard(String boardName) {
        if (!connected.get() || currentUserId == null) return false;
        
        CreateBoardPayload payload = new CreateBoardPayload(boardName);
        Message message = new Message("create_board", payload);
        out.println(message.toJson());
        
        try {
            String response = in.readLine();
            Message responseMsg = Message.fromJson(response);
            return "success".equals(responseMsg.getStatus());
        } catch (IOException e) {
            return false;
        }
    }
    
    public List<Board> getBoards() {
        List<Board> boards = new ArrayList<>();
        if (!connected.get() || currentUserId == null) {
            return boards;
        }
        
        // Add a small delay to ensure previous responses are processed
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Message message = new Message("list_boards", null);
        out.println(message.toJson());
        
        try {
            String response = in.readLine();
            Message responseMsg = Message.fromJson(response);
            
            if ("success".equals(responseMsg.getStatus()) && 
                "Boards retrieved successfully".equals(responseMsg.getMessage()) && 
                responseMsg.getData() != null) {
                
                String jsonData = responseMsg.getData().toString();
                boards = GsonUtil.createGson().fromJson(jsonData, 
                    new com.google.gson.reflect.TypeToken<List<Board>>(){}.getType());
            }
        } catch (Exception e) {
            // Silent error handling
        }
        
        return boards;
    }
    
    public boolean setCurrentBoard(String boardId) {
        if (!connected.get() || currentUserId == null) {
            return false;
        }
        
        Message message = new Message("view_board", boardId);
        out.println(message.toJson());
        
        try {
            String response = in.readLine();
            Message responseMsg = Message.fromJson(response);
            if ("success".equals(responseMsg.getStatus())) {
                currentBoardId = boardId;
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
    
    public boolean addTask(String title, String description, String priority) {
        if (!connected.get() || currentUserId == null || currentBoardId == null) {
            return false;
        }
        
        AddTaskPayload payload = new AddTaskPayload(title, description, priority);
        Message message = new Message("add_task", payload);
        out.println(message.toJson());
        
        try {
            String response = in.readLine();
            Message responseMsg = Message.fromJson(response);
            if ("success".equals(responseMsg.getStatus())) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
    
    public List<Task> getTasks(String boardId) {
        List<Task> tasks = new ArrayList<>();
        if (!connected.get() || currentUserId == null) return tasks;
        
        Message message = new Message("list_tasks", null);
        out.println(message.toJson());
        
        try {
            String response = in.readLine();
            Message responseMsg = Message.fromJson(response);
            if ("success".equals(responseMsg.getStatus()) && responseMsg.getData() != null) {
                // Parse tasks from response data
                String jsonData = responseMsg.getData().toString();
                tasks = GsonUtil.createGson().fromJson(jsonData, 
                    new com.google.gson.reflect.TypeToken<List<Task>>(){}.getType());
            }
        } catch (Exception e) {
        }
        
        return tasks;
    }
    
    public void disconnect() {
        connected.set(false);
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
        }
    }
}
