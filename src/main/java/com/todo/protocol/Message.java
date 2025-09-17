package com.todo.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;

/**
 * JSON message format for client-server communication
 * Simple wrapper around Gson for easy JSON parsing
 */
public class Message {
    @SerializedName("command")
    private String command;
    
    @SerializedName("payload")
    private Object payload;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("data")
    private Object data;

    public Message() {}

    public Message(String command, Object payload) {
        this.command = command;
        this.payload = payload;
    }

    public Message(String status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Getters and setters
    public String getCommand() { return command; }
    public Object getPayload() { return payload; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Object getData() { return data; }

    public void setCommand(String command) { this.command = command; }
    public void setPayload(Object payload) { this.payload = payload; }
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setData(Object data) { this.data = data; }

    /**
     * Parse JSON string to Message object
     */
    public static Message fromJson(String json) {
        try {
            Gson gson = createGson();
            return gson.fromJson(json, Message.class);
        } catch (JsonSyntaxException e) {
            return new Message("error", "Invalid JSON format", null);
        }
    }

    /**
     * Convert Message object to JSON string
     */
    public String toJson() {
        return createGson().toJson(this);
    }

    /**
     * Create success response
     */
    public static Message success(String message, Object data) {
        return new Message("success", message, data);
    }

    /**
     * Create error response
     */
    public static Message error(String message) {
        return new Message("error", message, null);
    }

    /**
     * Create Gson instance with LocalDateTime support
     */
    private static Gson createGson() {
        return new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new com.todo.util.GsonUtil.LocalDateTimeAdapter())
            .create();
    }
}
