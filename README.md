# Simple Todo List Application

A minimal, multi-threaded client-server Todo List application with real-time notifications and collaborative features.

## Features

- **Multi-threaded Server**: Handles multiple clients simultaneously
- **Real-time Notifications**: Instant updates to all connected clients
- **User Authentication**: Secure login with password hashing (SHA-256 + salt)
- **Collaborative Boards**: Users can create boards and invite others
- **Task Management**: Full CRUD operations for tasks
- **Thread-safe**: Concurrent access with proper synchronization
- **TCP + UDP**: Dual protocol support

## Project Structure

```
src/main/java/com/todo/
├── SimpleTodoServer.java    (205 lines) - Server implementation
└── SimpleTodoClient.java    (106 lines) - Client implementation
```

**Total: 311 lines of code** (down from 1,832 lines - 83% reduction!)

## How to Run

### 1. Compile the Project
```bash
javac -cp . src/main/java/com/todo/SimpleTodoServer.java src/main/java/com/todo/SimpleTodoClient.java
```

### 2. Start the Server
```bash
java -cp src/main/java com.todo.SimpleTodoServer
```

### 3. Start a Client
```bash
java -cp src/main/java com.todo.SimpleTodoClient
```

## Command Syntax

### Authentication Commands
```bash
register <username> <password>     # Register a new user
login <username> <password>        # Login to existing account
logout                             # Logout from current account
```

### Board Management Commands
```bash
create_board <boardName>           # Create a new board
list_boards                        # List all accessible boards
add_user_to_board <boardID> <username>  # Add user to board (owner only)
view_board <boardID>               # Enter board view mode
```

### Task Management Commands
*Note: These commands only work when in board view mode (after using `view_board`)*

```bash
add_task <title> <description>     # Add a new task to current board
list_tasks                         # List all tasks in current board
update_task_status <taskID> <status>  # Update task status (TODO, IN_PROGRESS, DONE)
delete_task <taskID>               # Delete a task from current board
```

### System Commands
```bash
exit                               # Exit the client
```

## Example Usage

### Terminal 1 (Server)
```bash
java -cp src/main/java com.todo.SimpleTodoServer
# Output: Starting Simple Todo Server...
# Output: Server started on ports 1234 and 4321
```

### Terminal 2 (Client 1)
```bash
java -cp src/main/java com.todo.SimpleTodoClient
> register alice password123
📋 SUCCESS: Registered
> login alice password123
📋 SUCCESS: Logged in
> create_board MyProject
📋 SUCCESS: Board created b1234567890
> list_boards
📋 b1234567890:MyProject
> add_user_to_board b1234567890 bob
📋 SUCCESS: User added
> view_board b1234567890
📋 SUCCESS: Viewing board MyProject
> add_task Fix Bug Description of the bug
📋 SUCCESS: Task added t1234567890
> list_tasks
📋 t1234567890:Fix Bug:TODO
> update_task_status t1234567890 DONE
📋 SUCCESS: Task updated
> logout
📋 SUCCESS: Logged out
> exit
```

### Terminal 3 (Client 2 - Bob)
```bash
java -cp src/main/java com.todo.SimpleTodoClient
> register bob password456
📋 SUCCESS: Registered
> login bob password456
📋 SUCCESS: Logged in
🔔 user_added: bob                    # Real-time notification from Alice's action
> list_boards
📋 b1234567890:MyProject
> view_board b1234567890
📋 SUCCESS: Viewing board MyProject
🔔 task_added: t1234567890:Fix Bug    # Real-time notification
🔔 task_updated: t1234567890:DONE     # Real-time notification
> list_tasks
📋 t1234567890:Fix Bug:DONE
```

## Real-time Notifications

The system sends instant notifications to all connected clients:

- `🔔 board_created: <boardID>` - When a new board is created
- `🔔 user_added: <username>` - When a user is added to a board
- `🔔 task_added: <taskID>:<title>` - When a task is added
- `🔔 task_updated: <taskID>:<status>` - When a task status is updated
- `🔔 task_deleted: <taskID>` - When a task is deleted

## Technical Details

- **Server Ports**: TCP 1234, UDP 4321
- **Thread Safety**: Uses `ConcurrentHashMap` for thread-safe operations
- **Password Security**: SHA-256 hashing with random salt per user
- **Architecture**: Multi-threaded server with dual-thread client (main + listener)
- **Protocol**: Simple text-based protocol over TCP for commands, UDP for notifications

## Requirements Met

✅ Multi-threaded server  
✅ Thread-safe concurrency management  
✅ User authentication with password hashing  
✅ Collaborative board management  
✅ Full CRUD operations for tasks  
✅ Real-time notifications  
✅ TCP and UDP protocols  
✅ CLI interface  
✅ Access control and permissions  
✅ Clean, minimal code structure  

## License

This project is created for educational purposes.