# Java Client-Server Chat Application

A multi-client chat application developed in Java, featuring a server that manages connections and a client with a graphical user interface built using Java Swing. The server leverages modern Java features, including virtual threads, to handle multiple clients concurrently and efficiently.

---

## Features

* [cite_start]**Multi-Client Architecture**: The server can handle numerous client connections simultaneously using a virtual thread per client. [cite: 1]
* **GUI Client**: An intuitive and user-friendly chat interface built with Java Swing.
* [cite_start]**Dynamic Server Configuration**: The server's port, name, and a list of banned phrases are loaded from an external `server_config.txt` file. [cite: 1]
* **Advanced Messaging Modes**:
    * **Global**: Send messages to all users in the chat room.
    * [cite_start]**Include**: Send private messages to a selected group of users. [cite: 1]
    * [cite_start]**Exclude**: Broadcast a message to all users except for a selected few. [cite: 1]
* [cite_start]**Content Moderation**: The server filters usernames and messages, rejecting any that contain predefined banned phrases. [cite: 1]
* [cite_start]**Real-Time User List**: Clients receive and display a continuously updated list of all connected users. [cite: 1]
* [cite_start]**On-Demand Information**: Clients can request and view the server's usage instructions and the list of banned words at any time. [cite: 1]

---

## Project Structure

The project is organized into two main parts: the server-side application and the client-side application.
```
Server-client-project/
├─ serverPart/
│  ├─ Server.java
│  └─ server_config.txt
└─ clientPart/
   └─ Client.java
```
---

## Getting Started

Follow these instructions to compile and run the project on your local machine.

### Prerequisites

* **Java Development Kit (JDK) 21** or newer is required to support virtual threads.

### Configuration

Before starting the server, you can configure its settings in the `serverPart/server_config.txt` file.

* `port`: The port number the server will listen on.
* `serverName`: The name of the server (e.g., localhost).
* `bannedPhrases`: A comma-separated list of words that are not allowed in usernames or messages.

**Example `server_config.txt`:**
port=12345 serverName=localhost bannedPhrases=java,SAD,JNI
### Installation and Execution

You must start the server first, followed by one or more clients.

**1. Prepare the Server Code**

> **Important:** Before compiling, you must modify one line in `Server.java` to correctly locate the configuration file.

* Open `Server.java`.
* Find the `main` method at the bottom of the file.
* Change this line:
    ```java
    Server server = new Server("ServerPart/src/server_config.txt");
    ```
* To this:
    ```java
    Server server = new Server("server_config.txt");
    ```

**2. Run the Server**

* Open a terminal and navigate to the server's directory:
    ```bash
    cd Server-client-project/serverPart
    ```
* Compile the `Server.java` file:
    ```bash
    javac Server.java
    ```
* Run the server:
    ```bash
    java Server
    ```
    The server will now be running and listening for client connections on the configured port.

**3. Run the Client**

* Open a **new** terminal and navigate to the client's directory:
    ```bash
    cd Server-client-project/clientPart
    ```
* Compile the `Client.java` file:
    ```bash
    javac Client.java
    ```
* Run the client:
    ```bash
    java Client
    ```
* Repeat this step to launch additional client instances.

---

## Usage Guide

1.  **Login**: When the client application starts, you will be prompted with a login window. Enter a unique **Username**, the **Server IP** (e.g., `localhost`), and the **Port** that the server is running on.
2.  **Select Messaging Mode**:
    * **Global**: Your message will be sent to every user in the chat. User checkboxes will be disabled.
    * **Include**: Your message will only be sent to the users you select from the user list.
    * **Exclude**: Your message will be sent to every user **except** those you select from the user list.
3.  **Sending Messages**: Type your message in the input field at the bottom and press **Enter**.
4.  **Additional Actions**:
    * **Show Banned Words**: Click this button to view the list of words that are filtered by the server.
    * **Disconnect**: Click this button to safely disconnect from the server and return to the login screen.
