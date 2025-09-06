# Todo List Application

A client-server Todo List management system built with Java, featuring real-time collaboration, secure user management, and persistent data storage.

## Features

- **User Management**: Secure registration and login with password hashing
- **Board Management**: Create and manage collaborative boards
- **Task Management**: Full CRUD operations for tasks with priorities and status tracking
- **Real-time Notifications**: Server pushes updates to all connected clients
- **Persistent Storage**: Data is saved to JSON files on the server
- **JSON Protocol**: All communication uses structured JSON messages

## Quick Start

### 1. Start the Server
- Open `src/main/java/com/todo/server/ServerMain.java`
- Click the **Run** button (▶️) or use `Ctrl+Shift+F10`
- You should see: `Server started on TCP port 8080 and UDP port 8081`

### 2. Start the Client
- Open `src/main/java/com/todo/client/ClientMain.java` 
- Click the **Run** button (▶️) or use `Ctrl+Shift+F10`
- You should see the client connect and show the command prompt

### 3. Test Commands
In the client, try these commands:
```
> register root toor
> login root toor
> create_board My Project
> add_task Complete report Write the quarterly report HIGH
> list_tasks
> help
> exit
```

## Client Commands

#### Authentication
- `register <username> <password>` - Register a new user
- `login <username> <password>` - Login to your account
- `logout` - Logout from current account

#### Board Management
- `create_board <boardName>` - Create a new board
- `list_boards` - List all your boards
- `add_user_to_board <boardID> <userID>` - Add user to board
- `view_board <boardID>` - Enter board view mode

#### Task Management (only in board view mode)
- `add_task <title> <description> <priority>` - Add new task
- `list_tasks` - List all tasks in current board
- `update_task_status <taskID> <status>` - Update task status
- `delete_task <taskID>` - Delete a task

#### Other
- `help` - Show available commands
- `exit` or `quit` - Exit the client

### Priority and Status Values
- **Priority**: LOW, MEDIUM, HIGH
- **Status**: TODO, IN_PROGRESS, DONE

## Project Structure

```
src/main/java/com/todo/
├── client/           # Client-side code
├── server/           # Server-side code
├── model/            # Data models (User, Board, Task)
├── protocol/         # JSON message protocol
├── storage/          # Data persistence layer
└── util/             # Utility classes
```

## Technical Details

- **Concurrency**: Uses ReadWriteLock for thread-safe data access
- **Security**: SHA-256 password hashing with salt
- **Real-time**: Server pushes notifications to relevant clients
- **Storage**: JSON file-based persistence with automatic loading/saving
- **Protocol**: JSON-based communication over TCP sockets