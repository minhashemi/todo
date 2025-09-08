package com.todo.server;

import com.todo.storage.DataStorage;

public class ServerMain {
    public static void main(String[] args) {
        DataStorage storage = new DataStorage();
        Server server = new Server(storage);
        
        // Add shutdown hook to gracefully stop the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));
        
        server.start();
    }
}
