package com.todo.client;

public class ClientMain {
    public static void main(String[] args) {
        System.out.println("=== Todo List Client ===");
        System.out.println("Connecting to server...");
        
        Client client = new Client();
        client.start();
    }
}
