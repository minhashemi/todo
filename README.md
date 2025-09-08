# Todo List Application

A client-server Todo List management system built with Java, featuring real-time collaboration, secure user management, and persistent data storage.

## Features

- **User Management**: Secure registration and login with password hashing
- **Board Management**: Create and manage collaborative boards
- **Task Management**: Full CRUD operations for tasks with priorities and status tracking
- **Real-time Notifications**: Server pushes updates to all connected clients
- **Persistent Storage**: Data is saved to SQLite database with JDBC
- **JSON Protocol**: All communication uses structured JSON messages

## Quick Start

### 1. Start the Server
```bash
mvn exec:java -Dexec.mainClass="com.todo.server.ServerMain"
```

### 2. Start the GUI Client
```bash
mvn exec:java -Dexec.mainClass="com.todo.gui.TodoGUI"
```

### 3. Alternative: Command Line Client
```bash
mvn exec:java -Dexec.mainClass="com.todo.client.ClientMain"
```

### 4. GUI Features
The GUI provides a simple interface with:
- **Login/Register Panel**: User authentication
- **Board Management**: Create and view boards
- **Task Management**: Add and view tasks with priorities
- **Status Bar**: Shows current user and connection status

### 5. Command Line Test Commands
For the command line client:
```
> register root toor
> login root toor
> create_board My Project
> list_boards
> view_board <boardID_from_list_boards>
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
├── client/           # Command line client
├── core/             # Core interfaces and design patterns
├── gui/              # Swing GUI client
├── model/            # Data models (User, Board, Task)
├── protocol/         # JSON message protocol
├── server/           # Server-side code
│   └── commands/     # Command pattern implementations
├── storage/          # Data persistence layer
└── util/             # Utility classes
```

## Database Implementation

The application uses SQLite database with JDBC for persistent storage:

- **Database**: `todo.db` (created automatically on first run)
- **Tables**: 
  - `users` - User accounts with secure password hashing
  - `boards` - Project boards with ownership tracking
  - `board_members` - Many-to-many relationship between users and boards
  - `tasks` - Tasks with priorities, status, and board association
- **Features**: Foreign key constraints, automatic table creation, thread-safe operations

## Technical Details

- **Concurrency**: Uses ReadWriteLock for thread-safe data access
- **Security**: SHA-256 password hashing with salt
- **Real-time**: Server pushes notifications to relevant clients
- **Storage**: SQLite database with JDBC for persistent data storage
- **Protocol**: JSON-based communication over TCP sockets