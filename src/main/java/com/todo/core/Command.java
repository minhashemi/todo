package com.todo.core;

import com.todo.protocol.Message;

public interface Command {
    Message execute(Object payload, String clientId);
}
