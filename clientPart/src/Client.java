import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Client {
    // Client's username
    private String username;

    // Networking components
    private Socket socket;
    private PrintWriter out; // For sending messages to the server
    private BufferedReader in; // For receiving messages from the server

    // GUI components for the login screen
    private JFrame loginFrame;

    // GUI components for the chat application
    private JFrame chatFrame;
    private JTextArea chatArea; // Area to display chat messages
    private JTextField inputField; // Input field for typing messages
    private JPanel userListPanel; // Panel to display list of online users
    private JLabel instructionsLabel;
    private JRadioButton globalRadioButton;
    private JRadioButton privateRadioButton;
    private JRadioButton excludeRadioButton;

    // Map to manage checkboxes for each user in the user list
    private Map<String, JCheckBox> userCheckBoxes;

    // Entry point of the client application
    public static void main(String[] args) {
        // Use SwingUtilities to ensure the UI is created on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> new Client().showLoginUI());
    }


    // Connect to the server with the specified username, server IP, and port.
    private void connectToServer(String username, String serverIP, int port) {
        try {
            // Set up the username and establish a socket connection to the server
            this.username = username;
            this.socket = new Socket(serverIP, port);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Initialize the map to manage user checkboxes
            userCheckBoxes = new HashMap<>();

            // Send the username to the server as the first message
            out.println(username);

            // Wait for a response from the server
            String response = in.readLine();
            if (response != null && response.startsWith("ERROR:")) {
                // If the server returns an error, display it and close the connection
                JOptionPane.showMessageDialog(loginFrame, response, "Error", JOptionPane.ERROR_MESSAGE);
                socket.close();
                return; // Stop the connection process
            }

            // Request instructions from the server automatically
            out.println("GET_INSTRUCTIONS");

            // Close the login window and open the chat UI
            loginFrame.dispose();
            showChatUI();

            // Start a virtual thread to listen for messages from the server
            Thread.ofVirtual().start(this::listenForMessages);
        } catch (IOException e) {
            // Handle connection errors
            JOptionPane.showMessageDialog(loginFrame, "Failed to connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    // Continuously listen for messages from the server.
    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("USER_LIST:")) {
                    // Update the user list if the server sends a user list
                    updateUserList(message.substring(10));
                } else if (message.startsWith("BANNED_WORDS:")) {
                    // Show a banned words window if the server sends banned words
                    String bannedWords = message.substring(13);
                    SwingUtilities.invokeLater(() -> showBannedWordsWindow(bannedWords));
                } else if (message.startsWith("INSTRUCTIONS:")) {
                    // Update the instructions label with server instructions
                    String instructions = message.substring(13);
                    SwingUtilities.invokeLater(() -> instructionsLabel.setText(instructions));
                } else {
                    // Display other messages in the chat area
                    chatArea.append(message + "\n");
                }
            }
        } catch (IOException e) {
            // Handle disconnection
            chatArea.append("Disconnected from server.\n");
        } finally {
            // Ensure proper cleanup upon disconnection
            disconnect();
        }
    }

    // Send a message to the server based on the selected message type.
    private void sendMessage() {
        String message = inputField.getText().trim(); // Get the typed message
        if (!message.isEmpty()) {
            if (globalRadioButton.isSelected()) {
                // Send the message to everyone
                out.println(message);
            } else if (privateRadioButton.isSelected()) {
                // Send the message to specific recipients
                StringBuilder recipients = new StringBuilder();
                for (Map.Entry<String, JCheckBox> entry : userCheckBoxes.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        recipients.append(entry.getKey()).append(",");
                    }
                }
                if (!recipients.isEmpty()) {
                    recipients.deleteCharAt(recipients.length() - 1); // Remove the comma at the end
                    out.println("INCLUDE:" + recipients + ":" + message);
                } else {
                    JOptionPane.showMessageDialog(chatFrame, "Select at least one recipient for private messaging.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if (excludeRadioButton.isSelected()) {
                // Exclude specific users from receiving the message
                StringBuilder excludedUsers = new StringBuilder();
                for (Map.Entry<String, JCheckBox> entry : userCheckBoxes.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        excludedUsers.append(entry.getKey()).append(",");
                    }
                }
                if (!excludedUsers.isEmpty()) {
                    excludedUsers.deleteCharAt(excludedUsers.length() - 1); // Remove the trailing comma
                }
                out.println("EXCLUDE:" + excludedUsers + ":" + message);
            }
            inputField.setText(""); // Clear the input field after sending
        }
    }


    // Update the user list panel based on the list received from the server.
    private void updateUserList(String users) {
        userListPanel.removeAll(); // Clear the existing user list
        userCheckBoxes.clear(); // Clear the checkbox map

        String[] userArray = users.split(","); // Split the list into individual usernames
        for (String user : userArray) {
            if (!user.equals(username)) {
                JCheckBox userCheckBox = new JCheckBox(user); // Create a checkbox for each user
                userCheckBox.setSelected(true); // Default to selected

                // Enable or disable the checkbox based on the selected message type
                if (globalRadioButton.isSelected()) {
                    userCheckBox.setEnabled(false); // Disable in "Global" mode
                } else {
                    userCheckBox.setEnabled(true); // Enable in other modes
                }

                // Add the checkbox to the user list panel
                userListPanel.add(userCheckBox);
                userCheckBoxes.put(user, userCheckBox); // Add to the map
            }
        }

        // Refresh the user list panel
        userListPanel.revalidate();
        userListPanel.repaint();
    }


    // Display the login UI for the user to input their credentials.
    private void showLoginUI() {
        loginFrame = new JFrame("Chat Client Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(300, 200);

        JPanel panel = new JPanel(new GridLayout(4, 2));
        JLabel userLabel = new JLabel("Username:");
        JTextField userField = new JTextField();
        JLabel serverLabel = new JLabel("Server IP:");
        JTextField serverField = new JTextField();
        JLabel portLabel = new JLabel("Port:");
        JTextField portField = new JTextField();

        JButton loginButton = new JButton("Login");
        // Validate user input and attempt connection
        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String serverIP = serverField.getText().trim();
            String portText = portField.getText().trim();

            if (username.isEmpty() || serverIP.isEmpty() || portText.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                int port = Integer.parseInt(portText);
                connectToServer(username, serverIP, port);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(loginFrame, "Port must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(userLabel);
        panel.add(userField);
        panel.add(serverLabel);
        panel.add(serverField);
        panel.add(portLabel);
        panel.add(portField);
        panel.add(new JLabel());
        panel.add(loginButton);

        loginFrame.add(panel);
        loginFrame.setVisible(true);
    }


    //Display the chat UI after successfully logging in.
    private void showChatUI() {
        chatFrame = new JFrame("Chat - " + username);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setSize(650, 600);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());

        // User list panel setup
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        JScrollPane userScroll = new JScrollPane(userListPanel);

        globalRadioButton = new JRadioButton("Global");
        privateRadioButton = new JRadioButton("Include");
        excludeRadioButton = new JRadioButton("Exclude");
        ButtonGroup messageTypeGroup = new ButtonGroup();
        messageTypeGroup.add(globalRadioButton);
        messageTypeGroup.add(privateRadioButton);
        messageTypeGroup.add(excludeRadioButton);

        globalRadioButton.setSelected(true); // Default to global messaging
        setCheckBoxEnabled(false);
        globalRadioButton.addActionListener(e -> {
            setCheckBoxEnabled(false);
            selectAllCheckBoxes(); // Automatically select all users when "Global" is selected
        });
        privateRadioButton.addActionListener(e -> setCheckBoxEnabled(true));
        excludeRadioButton.addActionListener(e -> setCheckBoxEnabled(true));

        JPanel messageTypePanel = new JPanel();
        messageTypePanel.setBorder(BorderFactory.createTitledBorder("Message Type"));
        messageTypePanel.add(globalRadioButton);
        messageTypePanel.add(privateRadioButton);
        messageTypePanel.add(excludeRadioButton);

        // Instructions panel setup using JLabel
        instructionsLabel = new JLabel();
        instructionsLabel.setVerticalAlignment(SwingConstants.TOP); // Align text to the top
        instructionsLabel.setBorder(BorderFactory.createTitledBorder("Instructions"));
        instructionsLabel.setPreferredSize(new Dimension(150, 170)); // Adjust size as needed
        instructionsLabel.setText("<html><p>Instructions will appear here...</p></html>");

        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(messageTypePanel, BorderLayout.NORTH);
        userScroll.setBorder(BorderFactory.createTitledBorder("Users: "));
        sidePanel.add(userScroll, BorderLayout.CENTER);

        // Buttons for additional actions
        JButton bannedWordsButton = new JButton("Show Banned Words");
        bannedWordsButton.addActionListener(e -> requestBannedWords());

        JButton disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> disconnect());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1));
        bannedWordsButton.setBorder(BorderFactory.createLineBorder(new Color(184, 207, 229), 1));
        bannedWordsButton.setBackground(new Color(238, 238, 238));
        disconnectButton.setBorder(BorderFactory.createLineBorder(new Color(184, 207, 229), 1));
        disconnectButton.setBackground(new Color(238, 238, 238));
        buttonPanel.add(bannedWordsButton);
        buttonPanel.add(disconnectButton);

        JPanel lowerPanel = new JPanel(new BorderLayout());
        lowerPanel.add(buttonPanel, BorderLayout.SOUTH);
        lowerPanel.add(instructionsLabel, BorderLayout.CENTER);

        sidePanel.add(lowerPanel, BorderLayout.SOUTH);
        sidePanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        inputField.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel leftPanel = new JPanel(new BorderLayout());
        chatScroll.setBorder(BorderFactory.createTitledBorder("Chat: "));
        chatArea.setBackground(new Color(238, 238, 238));
        chatArea.setCaretColor(new Color(238, 238, 238));
        leftPanel.add(chatScroll, BorderLayout.CENTER);
        leftPanel.add(inputField, BorderLayout.SOUTH);
        inputField.setBackground(new Color(238, 238, 238));
        inputField.setBorder(BorderFactory.createTitledBorder("Input message here: "));

        // Add components to the main frame
        chatFrame.add(leftPanel, BorderLayout.CENTER);
        chatFrame.add(sidePanel, BorderLayout.EAST);

        chatFrame.setVisible(true);
    }

    // Other helper methods
    private void selectAllCheckBoxes() {
        for (JCheckBox checkBox : userCheckBoxes.values()) {
            checkBox.setSelected(true);
        }
    }

    private void setCheckBoxEnabled(boolean enabled) {
        for (JCheckBox checkBox : userCheckBoxes.values()) {
            checkBox.setEnabled(enabled);
        }
    }

    private void requestBannedWords() {
        out.println("GET_BANNED_WORDS");
    }

    private void showBannedWordsWindow(String bannedWords) {
        JFrame bannedWordsFrame = new JFrame("Banned Words");
        bannedWordsFrame.setSize(300, 200);

        JTextArea bannedWordsArea = new JTextArea(bannedWords);
        bannedWordsArea.setEditable(false);
        bannedWordsArea.setCaretColor(Color.white);

        bannedWordsFrame.add(new JScrollPane(bannedWordsArea));
        bannedWordsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        bannedWordsFrame.setVisible(true);
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (chatFrame != null) {
            chatFrame.dispose();
        }
        SwingUtilities.invokeLater(this::showLoginUI); // Return to log in screen
    }
}