# Todo List Application

A CLI-based todo list application with client-server architecture.

## Run

```bash
# Start server (ports 9999 TCP, 9998 UDP)
mvn exec:java -Dexec.mainClass="com.todo.server.ServerMain"

# Start client (in another terminal)
mvn exec:java -Dexec.mainClass="com.todo.client.ClientMain"
```

**Note**: You can run multiple clients simultaneously - the server supports multiple concurrent connections.

## Commands

- `register <username> <password>`
- `login <username> <password>`
- `logout`
- `create_board <boardName>`
- `list_boards`
- `add_user_to_board <boardID> <userID>`
- `view_board <boardID>`
- `add_task <title> <description> <priority>` (in board view)
- `list_tasks` (in board view)
- `update_task_status <taskID> <status>` (in board view)
- `delete_task <taskID>` (in board view)
- `help`
- `exit`

**Priority**: LOW, MEDIUM, HIGH  
**Status**: TODO, IN_PROGRESS, DONE