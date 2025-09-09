package com.todo.server;

import com.todo.core.Command;
import com.todo.storage.DataStorageInterface;
import com.todo.model.Board;
import com.todo.protocol.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int TCP_PORT = 9999;
    private static final int UDP_PORT = 9998;
    
    private final DataStorageInterface storage;
    private final Map<String, PrintWriter> connectedClients;
    private final Map<String, String> userSessions;
    private final Map<String, String> clientToUser;
    private final Map<String, String> clientCurrentBoard;
    private final ExecutorService threadPool;
    private final CommandFactory commandFactory;
    private final NotificationService notificationService;
    private boolean running;

    public Server(DataStorageInterface storage) {
        this.storage = storage;
        this.connectedClients = new ConcurrentHashMap<>();
        this.userSessions = new ConcurrentHashMap<>();
        this.clientToUser = new ConcurrentHashMap<>();
        this.clientCurrentBoard = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.notificationService = new NotificationService(connectedClients, clientToUser, userSessions, storage, UDP_PORT);
        this.commandFactory = new CommandFactory(storage, userSessions, clientToUser, clientCurrentBoard, notificationService);
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
                String commandName = message.getCommand();
                Object payload = message.getPayload();

                Command command = commandFactory.getCommand(commandName);
                if (command == null) {
                    return Message.error("Unknown command: " + commandName);
                }
                
                return command.execute(payload, clientId);
            } catch (Exception e) {
                return Message.error("Server error: " + e.getMessage());
            }
        }

    }


    public void stop() {
        running = false;
        threadPool.shutdown();
        System.out.println("Server stopped");
    }
}
