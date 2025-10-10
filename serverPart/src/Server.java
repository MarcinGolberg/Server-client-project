import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Main server class that handles client connections and messaging
public class Server {
    private int port; // Port number for the server
    private String serverName; // Name of the server
    private Set<String> bannedPhrases; // Set of banned phrases
    private Map<String, PrintWriter> clients; // Map of connected clients
    private ExecutorService executorService; // Thread pool for handling client connections

    // Constructor to initialize the server with a configuration file
    public Server(String configFilePath) throws IOException {
        this.clients = new HashMap<>();
        this.executorService = Executors.newVirtualThreadPerTaskExecutor(); // Virtual threads as per requirement
        loadConfiguration(configFilePath); // Load settings from the config file
    }

    // Load server configuration from the provided file path
    private void loadConfiguration(String configFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            this.port = Integer.parseInt(reader.readLine().split("=")[1].trim());
            this.serverName = reader.readLine().split("=")[1].trim();
            this.bannedPhrases = new HashSet<>(Arrays.asList(reader.readLine().split("=")[1].split(",")));
        }
    }

    // Start the server and listen for incoming client connections
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(serverName + " running on port " + port);

            // Infinite loop to accept client connections
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept a new client
                executorService.submit(new ClientHandler(clientSocket)); // Handle client in a new thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown(); // Shutdown the thread pool on server closure
        }
    }

    // Inner class to handle individual client communication
    private class ClientHandler implements Runnable {
        private Socket socket;
        private String clientName; // Name of the connected client
        private PrintWriter out; // Output stream to the client

        // Constructor accepting a client socket
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.clientName = in.readLine(); // First message is the client's username

                // Check if the username contains banned phrases
                List<String> bannedPhrasesInName = containsBannedPhrases(clientName);
                if (bannedPhrasesInName != null) {
                    // Inform the client about banned words in their username and close the connection
                    out.println("ERROR: Username contains banned word(s): " + String.join(", ", bannedPhrasesInName));
                    socket.close();
                    return;
                }

                synchronized (clients) {
                    clients.put(clientName, out); // Add client to the active clients map
                    broadcast(clientName + " has joined the chat!", null, null); // Notify other clients
                    broadcastUserList(); // Update all clients with the current user list
                }

                // Handle messages from the client
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("GET_BANNED_WORDS")) {
                        // Send the list of banned words
                        out.println("BANNED_WORDS:" + String.join(",", bannedPhrases));
                    } else if (message.equals("GET_INSTRUCTIONS")) {
                        // Send usage instructions
                        out.println("INSTRUCTIONS:<html>"
                                + "<p>Type your message in the box in bottom left corner of window.</p>"
                                + "<p>Select what type of message you want to send:</p>"
                                + "<p> ---global = send to everyone</p>"
                                + "<p> ---include = check box next to users that WILL see your message.</p>"
                                + "<p> ---exclude = check box next to users that WON'T see your message.</p>"
                                + "</html>");
                    } else {
                        // Check if the message contains banned phrases
                        List<String> bannedPhrasesInMessage = containsBannedPhrases(message);
                        if (bannedPhrasesInMessage != null) {
                            // Inform client about banned phrases
                            out.println("BANNED: Message contains banned phrases: " + String.join(", ", bannedPhrasesInMessage));
                        } else if (message.startsWith("EXCLUDE:")) {
                            // Handle messages to be excluded for certain users
                            handleExclusionMessage(message);
                        } else if (message.startsWith("INCLUDE:")) {
                            // Handle messages for specific included users
                            handleInclusionMessage(message);
                        } else {
                            // Broadcast the message globally
                            broadcast(message, clientName, null);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnectClient(); // Ensure proper cleanup on client disconnect
            }
        }

        // Check if a given text contains banned phrases
        private List<String> containsBannedPhrases(String text) {
            List<String> foundBannedPhrases = new ArrayList<>();
            for (String phrase : bannedPhrases) {
                if (text.contains(phrase)) {
                    foundBannedPhrases.add(phrase); // Add each found banned phrase
                }
            }
            return foundBannedPhrases.isEmpty() ? null : foundBannedPhrases;
        }

        // Broadcast the current list of connected users to all clients
        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder("USER_LIST:");
            for (String client : clients.keySet()) {
                userList.append(client).append(",");
            }
            if (userList.length() > 0 && userList.charAt(userList.length() - 1) == ',') {
                userList.deleteCharAt(userList.length() - 1);
            }
            broadcast(userList.toString(), null, null);
        }

        // Send a message to all connected clients, excluding specified users
        private void broadcast(String message, String sender, Set<String> excludedUsers) {
            synchronized (clients) {
                for (String recipient : clients.keySet()) {
                    if (excludedUsers != null && excludedUsers.contains(recipient)) {
                        continue; // Skip excluded users
                    }
                    PrintWriter clientOut = clients.get(recipient);
                    if (clientOut != null) {
                        clientOut.println((sender != null ? sender + ": " : "") + message);
                    }
                }
            }
        }

        // Handle messages explicitly sent to specific included users
        private void handleInclusionMessage(String message) {
            message = message.substring(8).trim(); // Remove "INCLUDE:" prefix
            String[] parts = message.split(":", 2); // Split into excluded users and message content
            if (parts.length == 2) {
                String[] recipientList = parts[0].split(",");
                String content = parts[1];
                Set<String> recipients = new HashSet<>(Arrays.asList(recipientList));

                synchronized (clients) {
                    for (String recipient : recipients) {
                        PrintWriter recipientOut = clients.get(recipient.trim());
                        if (recipientOut != null) {
                            recipientOut.println(clientName + ": " + content);
                        }
                    }
                }
                out.println(clientName + ": " + content);
            } else {
                out.println("ERROR: Invalid INCLUDE message format.");
            }
        }

        // Handle messages explicitly excluding certain users
        private void handleExclusionMessage(String message) {
            message = message.substring(8).trim(); // Remove "EXCLUDE:" prefix
            String[] parts = message.split(":", 2); // Split into excluded users and message content

            if (parts.length == 2) {
                String[] excludedUserList = parts[0].split(","); // Get excluded users
                String content = parts[1]; // Get the message content

                Set<String> excludedUsers = new HashSet<>(Arrays.asList(excludedUserList));

                // Call broadcast with the excluded users
                broadcast(content, clientName, excludedUsers);

            } else {
                out.println("ERROR: Invalid EXCLUDE message format. Correct format is EXCLUDE:username1,username2:message");
            }
        }

        // Disconnect a client and clean up resources
        private void disconnectClient() {
            if (clientName != null) {
                synchronized (clients) {
                    clients.remove(clientName);
                    if (containsBannedPhrases(clientName) == null) {
                        broadcast(clientName + " has left the chat.", null, null);
                    }
                    broadcastUserList(); // Update the user list for other clients
                }
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Main method to start the server
    public static void main(String[] args) throws IOException {
        Server server = new Server("ServerPart/src/server_config.txt");
        server.start();
    }
}
