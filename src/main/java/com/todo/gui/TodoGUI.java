package com.todo.gui;

import com.todo.gui.SimpleClient;
import com.todo.model.Board;
import com.todo.model.Task;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TodoGUI extends JFrame {
    private SimpleClient client;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private String currentBoardId;
    private Timer autoRefreshTimer;
    
    // Panel references for easy switching
    private JPanel loginPanel;
    private JPanel boardPanel;
    private JPanel taskPanel;
    
    public TodoGUI() {
        setTitle("Todo List Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        
        // Initialize client
        client = new SimpleClient();
        
        // Create main layout
        setLayout(new BorderLayout());
        
        // Main panel with CardLayout for smooth switching
        mainPanel = new JPanel(new CardLayout());
        add(mainPanel, BorderLayout.CENTER);
        
        // Status bar
        statusLabel = new JLabel("Not connected");
        add(statusLabel, BorderLayout.SOUTH);
        
        // Create all panels
        createLoginPanel();
        createBoardPanel();
        createTaskPanel();
        
        // Start with login panel
        showPanel("LOGIN");
    }
    
    private void createLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Title
        JLabel titleLabel = new JLabel("Todo List Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 20, 10);
        loginPanel.add(titleLabel, gbc);
        
        // Username field
        gbc.gridwidth = 1; gbc.gridy = 1; gbc.gridx = 0;
        gbc.insets = new Insets(5, 10, 5, 5);
        loginPanel.add(new JLabel("Username:"), gbc);
        JTextField usernameField = new JTextField(15);
        gbc.gridx = 1; gbc.insets = new Insets(5, 5, 5, 10);
        loginPanel.add(usernameField, gbc);
        
        // Password field
        gbc.gridy = 2; gbc.gridx = 0; gbc.insets = new Insets(5, 10, 5, 5);
        loginPanel.add(new JLabel("Password:"), gbc);
        JPasswordField passwordField = new JPasswordField(15);
        gbc.gridx = 1; gbc.insets = new Insets(5, 5, 5, 10);
        loginPanel.add(passwordField, gbc);
        
        // Buttons
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        gbc.gridy = 3; gbc.gridx = 0; gbc.insets = new Insets(20, 10, 10, 5);
        loginPanel.add(loginBtn, gbc);
        gbc.gridx = 1; gbc.insets = new Insets(20, 5, 10, 10);
        loginPanel.add(registerBtn, gbc);
        
        // Button actions
        loginBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (client.login(username, password)) {
                statusLabel.setText("Logged in as: " + username);
                startAutoRefresh();
                showPanel("BOARD");
            } else {
                JOptionPane.showMessageDialog(this, "Login failed!");
            }
        });
        
        registerBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (client.register(username, password)) {
                JOptionPane.showMessageDialog(this, "Registration successful!");
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed!");
            }
        });
        
        mainPanel.add(loginPanel, "LOGIN");
    }
    
    private void createBoardPanel() {
        boardPanel = new JPanel(new BorderLayout());
        
        // Top panel with board creation
        JPanel topPanel = new JPanel(new FlowLayout());
        JTextField boardNameField = new JTextField(20);
        JButton createBoardBtn = new JButton("Create Board");
        
        topPanel.add(new JLabel("Board Name:"));
        topPanel.add(boardNameField);
        topPanel.add(createBoardBtn);
        
        // Board list
        JList<String> boardList = new JList<>();
        JScrollPane scrollPane = new JScrollPane(boardList);
        
        // Bottom panel
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton viewBoardBtn = new JButton("View Board");
        JButton logoutBtn = new JButton("Logout");
        bottomPanel.add(viewBoardBtn);
        bottomPanel.add(logoutBtn);
        
        // Add panels
        boardPanel.add(topPanel, BorderLayout.NORTH);
        boardPanel.add(scrollPane, BorderLayout.CENTER);
        boardPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Actions
        createBoardBtn.addActionListener(e -> {
            String boardName = boardNameField.getText();
            if (!boardName.isEmpty()) {
                if (client.createBoard(boardName)) {
                    boardNameField.setText("");
                    statusLabel.setText("Board created: " + boardName);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to create board!");
                }
            }
        });
        
        viewBoardBtn.addActionListener(e -> {
            String selectedBoard = boardList.getSelectedValue();
            if (selectedBoard != null) {
                currentBoardId = selectedBoard.split(" - ")[0]; // Extract board ID
                if (client.setCurrentBoard(currentBoardId)) {
                    showPanel("TASK");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to view board! Check console for errors.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a board first!");
            }
        });
        
        logoutBtn.addActionListener(e -> {
            stopAutoRefresh();
            client.logout();
            showPanel("LOGIN");
        });
        
        mainPanel.add(boardPanel, "BOARD");
    }
    
    private void createTaskPanel() {
        taskPanel = new JPanel(new BorderLayout());
        
        // Top panel with task creation
        JPanel topPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        topPanel.add(new JLabel("Title:"));
        JTextField titleField = new JTextField();
        topPanel.add(titleField);
        topPanel.add(new JLabel("Priority:"));
        JComboBox<String> priorityCombo = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH"});
        topPanel.add(priorityCombo);
        JButton addTaskBtn = new JButton("Add Task");
        topPanel.add(addTaskBtn);
        
        // Task list
        JList<String> taskList = new JList<>();
        JScrollPane scrollPane = new JScrollPane(taskList);
        
        // Bottom panel
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton backBtn = new JButton("Back to Boards");
        bottomPanel.add(backBtn);
        
        // Add panels
        taskPanel.add(topPanel, BorderLayout.NORTH);
        taskPanel.add(scrollPane, BorderLayout.CENTER);
        taskPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Actions
        addTaskBtn.addActionListener(e -> {
            String title = titleField.getText();
            String priority = (String) priorityCombo.getSelectedItem();
            if (!title.isEmpty()) {
                if (client.addTask(title, "Description", priority)) {
                    titleField.setText("");
                    statusLabel.setText("Task added: " + title);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add task!");
                }
            }
        });
        
        backBtn.addActionListener(e -> showPanel("BOARD"));
        
        mainPanel.add(taskPanel, "TASK");
    }
    
    private void showPanel(String panelName) {
        CardLayout layout = (CardLayout) mainPanel.getLayout();
        layout.show(mainPanel, panelName);
    }
    
    private void startAutoRefresh() {
        autoRefreshTimer = new Timer();
        autoRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    refreshAllData();
                });
            }
        }, 1000, 3000); // Start after 1 second, then refresh every 3 seconds
    }
    
    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.cancel();
            autoRefreshTimer = null;
        }
    }
    
    private void refreshAllData() {
        // Refresh board list
        if (boardPanel != null) {
            JScrollPane scrollPane = (JScrollPane) boardPanel.getComponent(1);
            JList<String> boardList = (JList<String>) scrollPane.getViewport().getView();
            refreshBoardList(boardList);
        }
        
        // Refresh task list
        if (taskPanel != null && currentBoardId != null) {
            JScrollPane scrollPane = (JScrollPane) taskPanel.getComponent(1);
            JList<String> taskList = (JList<String>) scrollPane.getViewport().getView();
            refreshTaskList(taskList);
        }
    }
    
    private void refreshBoardList(JList<String> boardList) {
        List<Board> boards = client.getBoards();
        String[] boardStrings = boards.stream()
            .map(board -> board.getId() + " - " + board.getName())
            .toArray(String[]::new);
        boardList.setListData(boardStrings);
    }
    
    private void refreshTaskList(JList<String> taskList) {
        List<Task> tasks = client.getTasks(currentBoardId);
        String[] taskStrings = tasks.stream()
            .map(task -> task.getTitle() + " (" + task.getPriority() + ") - " + task.getStatus())
            .toArray(String[]::new);
        taskList.setListData(taskStrings);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TodoGUI().setVisible(true);
        });
    }
}
