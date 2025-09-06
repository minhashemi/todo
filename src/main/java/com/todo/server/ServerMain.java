package com.todo.server;

public class ServerMain {
    public static void main(String[] args) {
        Server server = new Server();
        
        // Add shutdown hook to gracefully stop the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));
        
        server.start();
    }
}
