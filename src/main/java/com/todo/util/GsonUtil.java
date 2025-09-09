package com.todo.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSSSSS]");
    
    public static Gson createGson() {
        return new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    }
    
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(FORMATTER));
            }
        }
        
        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            } else {
                String dateString = in.nextString();
                try {
                    // Try ISO format first (most common)
                    return LocalDateTime.parse(dateString);
                } catch (Exception e1) {
                    try {
                        // Try the main format
                        return LocalDateTime.parse(dateString, FORMATTER);
                    } catch (Exception e2) {
                        try {
                            // Try format without microseconds
                            DateTimeFormatter simpleFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                            return LocalDateTime.parse(dateString, simpleFormatter);
                        } catch (Exception e3) {
                            try {
                                // Try format with just date and hour
                                DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH");
                                return LocalDateTime.parse(dateString, hourFormatter);
                            } catch (Exception e4) {
                                try {
                                    // Try to parse just the date part if time is truncated
                                    if (dateString.length() >= 10) {
                                        String datePart = dateString.substring(0, 10);
                                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                                        return LocalDateTime.parse(datePart + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                    }
                                } catch (Exception e5) {
                                    System.err.println("Failed to parse date: " + dateString);
                                }
                                // Return current time as fallback
                                return LocalDateTime.now();
                            }
                        }
                    }
                }
            }
        }
    }
}
